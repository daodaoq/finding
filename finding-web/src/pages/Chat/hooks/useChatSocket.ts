import { useEffect, useRef, useState } from 'react';
import { useWebSocket, useWsReconnect } from '../../../hooks/useWebSocket';
import type { User } from '../../../types/user';
import { wsToChatMessage, type ChatMessage } from '../types';

interface Options {
  targetUserId: number;
  user?: User | null;
  /** 会话消息 setter(来自 useChatSession),用于追加/撤回本地消息 */
  setMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>;
  /** 断线补拉(来自 useChatSession) */
  refreshFromServer: () => Promise<void>;
}

/**
 * 实时消息层:WebSocket 订阅(撤回/输入中/新消息按 messageId 去重)、
 * 断线重连后补拉、应用回前台刷新。返回对方"正在输入"状态。
 */
export function useChatSocket({ targetUserId, user, setMessages, refreshFromServer }: Options) {
  const [isTyping, setIsTyping] = useState(false);
  const typingTimerRef = useRef<ReturnType<typeof setTimeout>>();

  // WebSocket 实时消息处理(连接由全局单例管理;回调经 ref 始终指向最新闭包)
  useWebSocket((wsMsg) => {
    // 消息撤回:仅更新本地对应消息,不做删除;只处理私聊撤回(action='private'),避免群聊撤回的 messageId 串扰
    if (wsMsg.type === 'message_recalled' && wsMsg.action === 'private' && wsMsg.messageId) {
      setMessages((prev) => prev.map((m) =>
        m.id === wsMsg.messageId ? { ...m, isRecalled: 1, content: '该消息已撤回' } : m));
      return;
    }
    // 对方正在输入…(3 秒后自动消失)
    if (wsMsg.type === 'typing' && wsMsg.fromUserId === targetUserId) {
      setIsTyping(true);
      if (typingTimerRef.current) clearTimeout(typingTimerRef.current);
      typingTimerRef.current = setTimeout(() => setIsTyping(false), 3000);
      return;
    }
    // 接收对方消息 + 自己其他设备发送的消息(多端同步);按真实 messageId 去重
    const isRelevant = wsMsg.type === 'chat' && wsMsg.messageId
      && (wsMsg.fromUserId === targetUserId || wsMsg.fromUserId === user?.id);
    if (isRelevant) {
      setMessages((prev) => {
        if (prev.some((m) => m.id === wsMsg.messageId)) return prev;
        return [...prev, wsToChatMessage(wsMsg)];
      });
    }
  });

  // 断线补偿:WS 重连成功后补拉当前会话
  useWsReconnect(refreshFromServer);
  // 应用切回前台时刷新
  useEffect(() => {
    const onVisible = () => { if (document.visibilityState === 'visible') refreshFromServer(); };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [refreshFromServer]);

  return { isTyping };
}
