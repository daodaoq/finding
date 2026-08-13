import { useCallback, useEffect, useState } from 'react';
import { bridgeApi } from '../../../api/bridge';
import { chatApi } from '../../../api/chat';
import { useInfoShareStore } from '../../../store/infoShareStore';
import { useStaleGuard, isStaleError } from '../../../hooks/useStaleGuard';
import { showToast } from '../../../components/Toast';
import { getErrorMessage } from '../../../utils/appError';
import type { User } from '../../../types/user';
import type { Conversation } from '../../../types/message';
import type { InfoShareStatus } from '../../../types/resume';
import type { MessageLike } from '../components/MessageList';
import type { ChatMessage } from '../types';

interface Options {
  targetUserId: number;
  user?: User | null;
  conversation: Conversation | null;
  setMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>;
}

/** 模块级自增序号:临时消息 ID 同毫秒连续发送时递增,避免互相覆盖 */
let tempIdSeq = 0;

/** 回复/引用目标(输入栏显示引用条) */
export interface ReplyTarget {
  id: number;
  content: string;
  nickname: string;
}

/**
 * 用户动作层:发送/重试/撤回消息与信息互换(状态查询、发起、同意/拒绝)。
 * 持有回复引用(replyTo)与互换按钮状态;发送时以 roomId 定位会话。
 */
export function useChatActions({ targetUserId, user, conversation, setMessages }: Options) {
  const shareVersion = useInfoShareStore((s) => s.version);
  const [shareStatus, setShareStatus] = useState<InfoShareStatus['status']>('none');
  const [shareId, setShareId] = useState<number | null>(null);
  const [replyTo, setReplyTo] = useState<ReplyTarget | null>(null);
  const shareGuard = useStaleGuard();

  // 拉取与对方的「信息互换」状态(header 按钮);互换事件后通过全局 version 自动刷新
  useEffect(() => {
    if (!targetUserId || !user) return;
    const { promise } = shareGuard.run((signal) => bridgeApi.infoShareStatus(targetUserId, signal));
    promise
      .then((res) => {
        setShareStatus(res.data.data.status);
        setShareId(res.data.data.shareId);
      })
      .catch((e) => { if (!isStaleError(e)) { /* 静默:非过期错误由拦截器统一提示 */ } });
  }, [targetUserId, user, shareVersion, shareGuard.run]);

  // 发起互换申请
  const handleRequestShare = useCallback(async () => {
    try {
      await bridgeApi.infoShareRequest(targetUserId);
      setShareStatus('pendingSent');
      showToast('已发送互换申请，等待对方同意');
    } catch (e) {
      showToast(getErrorMessage(e, '发送失败，请重试'));
    }
  }, [targetUserId]);

  // 同意/拒绝互换(对方申请的状态下)
  const handleShareDecision = useCallback(async (approve: boolean) => {
    if (!shareId) return;
    try {
      await bridgeApi.infoShareHandle(shareId, approve ? 1 : 2);
      setShareStatus(approve ? 'approved' : 'none');
      showToast(approve ? '已同意互换详细信息' : '已拒绝互换申请');
    } catch (e) {
      showToast(getErrorMessage(e, '操作失败，请重试'));
    }
  }, [shareId]);

  // 撤回自己发送的消息
  const recallMessage = useCallback(async (msg: MessageLike) => {
    try {
      await chatApi.recallMessage(msg.id);
      setMessages((prev) => prev.map((m) => m.id === msg.id ? { ...m, isRecalled: 1, content: '该消息已撤回' } : m));
      showToast('已撤回');
    } catch (e) {
      showToast(getErrorMessage(e, '撤回失败'));
    }
  }, [setMessages]);

  // 发送消息:先插入本地临时消息,成功后用真实回执替换(避免与 WS 推送重复;
  // 同一 clientMessageId 重试返回同一条)
  const sendMessage = useCallback(async (content: string, messageType = 'text') => {
    if (!user || !conversation) return;
    const roomId = conversation.roomId || conversation.id;
    if (!roomId) return;
    const tempId = Date.now() * 1000 + (tempIdSeq++ % 1000);
    const newMsg: ChatMessage = {
      id: tempId, // 临时 ID
      fromUserId: user.id,
      toUserId: targetUserId,
      content,
      messageType,
      isRead: 0,
      createdAt: new Date().toISOString(),
      clientMessageId: `${user.id}_${tempId}`,
      parentMessageId: replyTo?.id,
      sendState: 'sending',
    };
    setMessages((prev) => [...prev, newMsg]);

    try {
      const res = await chatApi.sendMessage({
        roomId, content, messageType, clientMessageId: newMsg.clientMessageId, replyToMessageId: replyTo?.id,
      });
      const real = res.data.data;
      setMessages((prev) =>
        prev.filter((m) => m.id !== real.id).map((m) =>
          m.id === tempId
            ? { ...m, id: real.id, createdAt: real.createdAt, isRead: real.isRead ?? 0, sendState: 'sent' as const }
            : m));
    } catch (e) {
      // 失败不删除:标记失败,可点击重试
      setMessages((prev) => prev.map((m) => m.id === tempId ? { ...m, sendState: 'failed' as const } : m));
      showToast(getErrorMessage(e, '发送失败，请稍后重试'));
    }
    // 无论成败都清除引用条(失败消息保留 parentMessageId,重试时复用)
    setReplyTo(null);
  }, [user, conversation, targetUserId, replyTo, setMessages]);

  // 重试失败的发送(复用同一 clientMessageId,服务端幂等去重)
  const retryMessage = useCallback(async (msg: MessageLike) => {
    const failed = msg as ChatMessage;
    if (!conversation) return;
    const roomId = conversation.roomId || conversation.id;
    if (!roomId) return;
    setMessages((prev) => prev.map((m) => m.id === failed.id ? { ...m, sendState: 'sending' as const } : m));
    try {
      const res = await chatApi.sendMessage({
        roomId, content: failed.content, messageType: failed.messageType,
        clientMessageId: failed.clientMessageId, replyToMessageId: failed.parentMessageId,
      });
      const real = res.data.data;
      setMessages((prev) =>
        prev.filter((m) => m.id !== real.id).map((m) =>
          m.id === failed.id
            ? { ...m, id: real.id, createdAt: real.createdAt, isRead: real.isRead ?? 0, sendState: 'sent' as const }
            : m));
    } catch (e) {
      setMessages((prev) => prev.map((m) => m.id === failed.id ? { ...m, sendState: 'failed' as const } : m));
      showToast(getErrorMessage(e, '重试失败，请稍后再试'));
    }
  }, [conversation, setMessages]);

  return {
    shareStatus,
    shareId,
    replyTo,
    setReplyTo,
    handleRequestShare,
    handleShareDecision,
    sendMessage,
    retryMessage,
    recallMessage,
  };
}
