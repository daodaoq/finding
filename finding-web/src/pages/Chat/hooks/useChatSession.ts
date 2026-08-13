import { useCallback, useEffect, useRef, useState } from 'react';
import { chatApi } from '../../../api/chat';
import { useStaleGuard, isStaleError } from '../../../hooks/useStaleGuard';
import { useMessageStore } from '../../../store/messageStore';
import { showToast } from '../../../components/Toast';
import { getErrorMessage } from '../../../utils/appError';
import type { User } from '../../../types/user';
import type { Conversation, ChatSettings } from '../../../types/message';
import { toMsg, type ChatMessage } from '../types';

interface Options {
  targetUserId: number;
  user?: User | null;
  /** 初始化失败(无会话/无权限,已提示)后由页面回退上一页 */
  onInitError?: () => void;
}

/**
 * 会话核心状态:创建/获取会话、消息历史、游标分页、断线补拉、会话设置。
 * 竞态守卫:切换 userId 时自动取消旧请求;初始化与设置两个资源互不打断。
 * 对方昵称/头像不在此管理 —— 由页面从 conversation 派生(服务端为准)。
 */
export function useChatSession({ targetUserId, user, onInitError }: Options) {
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [chatSettings, setChatSettings] = useState<ChatSettings | null>(null);

  const initGuard = useStaleGuard();
  const settingsGuard = useStaleGuard();
  // 用 ref 保存 onInitError,避免回调身份变化导致 init 反复重建
  const onInitErrorRef = useRef(onInitError);
  onInitErrorRef.current = onInitError;

  // 初始化:获取或创建会话 + 加载消息历史。新会话只能由『相亲桥』聊天申请批准建立。
  const init = useCallback(async (signal?: AbortSignal) => {
    try {
      setLoading(true);
      const convRes = await chatApi.getOrCreateConversation(targetUserId, signal);
      if (signal?.aborted) return;
      const conv = convRes.data.data;
      setConversation(conv);
      // 使用 roomId 加载消息历史
      const roomId = conv.roomId || conv.id;
      const msgRes = await chatApi.getMessageHistory(roomId, undefined, undefined, signal);
      if (signal?.aborted) return;
      setMessages((msgRes.data.data.records || []).map(toMsg));
      // 进入会话已标记已读 → 刷新汇总角标
      useMessageStore.getState().refreshTotal();
    } catch (e) {
      // 被新请求/卸载取消:静默忽略
      if (isStaleError(e)) return;
      console.error('初始化会话失败', e);
      showToast(getErrorMessage(e, '还没有会话，请先通过『相亲桥』发起聊天申请'));
      // 会话不存在或无权限:抛出让调用方(init effect)触发 onInitError 回退
      throw e;
    }
  }, [targetUserId]);

  useEffect(() => {
    const { promise, isCurrent } = initGuard.run((signal) => init(signal));
    // 非过期失败(无会话/无权限):回退上一页
    promise.catch((e) => {
      if (!isStaleError(e)) onInitErrorRef.current?.();
    });
    // 只有最新请求才允许关闭 loading,避免切换会话时旧请求(被 abort)的收尾提前关掉新请求的 loading
    promise.finally(() => {
      if (isCurrent()) setLoading(false);
    });
  }, [targetUserId, initGuard.run, init]);

  // 向上滚动加载更早的消息(游标分页,lastId = 当前最小消息ID)
  const loadOlder = useCallback(async () => {
    if (loadingMore || !hasMore) return;
    const roomId = conversation?.roomId || conversation?.id;
    if (!roomId) return;
    const minId = messages.length ? Math.min(...messages.map((m) => m.id)) : undefined;
    setLoadingMore(true);
    try {
      const res = await chatApi.getMessageHistory(roomId, minId, 50);
      const older = (res.data.data.records || []).map(toMsg);
      setMessages((prev) => {
        const merged = [...older, ...prev];
        const seen = new Set<number>();
        return merged.filter((m) => (seen.has(m.id) ? false : (seen.add(m.id), true)));
      });
      // 游标分页:后端"多查一条"判定 hasMore,不再按 50 条整页猜测
      setHasMore(!!res.data.data.hasMore);
    } catch { /* 忽略 */ }
    finally { setLoadingMore(false); }
  }, [conversation, messages, loadingMore, hasMore]);

  // 断线补偿/回前台:拉取最新 50 条,按 id 合并去重(补回断线期间缺失的消息)
  const refreshFromServer = useCallback(async () => {
    const roomId = conversation?.roomId || conversation?.id;
    if (!roomId || !user) return;
    try {
      const res = await chatApi.getMessageHistory(roomId);
      const fresh = (res.data.data.records || []).map(toMsg);
      setMessages((prev) => {
        const merged = [...prev];
        for (const m of fresh) {
          if (!merged.some((x) => x.id === m.id)) merged.push(m);
        }
        return merged.sort((a, b) => a.id - b.id);
      });
      useMessageStore.getState().refreshTotal();
    } catch { /* 忽略 */ }
  }, [conversation, user]);

  // 拉取会话设置(聊天背景)
  useEffect(() => {
    const roomId = conversation?.roomId || conversation?.id;
    if (!roomId) return;
    const { promise } = settingsGuard.run((signal) => chatApi.getSettings(roomId, signal));
    promise.then((res) => setChatSettings(res.data.data))
      .catch((e) => { if (!isStaleError(e)) { /* 静默 */ } });
  }, [conversation, settingsGuard.run]);

  return {
    conversation,
    messages,
    setMessages,
    loading,
    hasMore,
    loadingMore,
    chatSettings,
    loadOlder,
    refreshFromServer,
  };
}
