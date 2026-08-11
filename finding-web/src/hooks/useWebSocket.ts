import { useEffect, useRef } from 'react';
import { chatSocket } from '../ws/chatSocket';
import type { WsMessage } from '../ws/chatSocket';

export type { WsMessage };

/**
 * 订阅"连接建立/重连成功"事件,用于断线补偿(补拉断线期间丢失的消息)。
 */
export function useWsReconnect(handler: () => void) {
  const ref = useRef(handler);
  ref.current = handler;
  useEffect(() => chatSocket.onReconnect(() => ref.current()), []);
}

/**
 * WebSocket 订阅 Hook —— 连接生命周期由全局单例 chatSocket 管理。
 *
 * 组件挂载时订阅回调并按需连接(幂等);卸载仅退订,不断开全局连接(避免路由切换遗留后台连接)。
 * @param enabled 登录态;为 false 时断开全局连接(仅登录态监听者传此参数,如 MainLayout)
 */
export function useWebSocket(onMessage: (msg: WsMessage) => void, enabled = true) {
  const onMsgRef = useRef(onMessage);
  onMsgRef.current = onMessage; // 始终指向最新回调,避免回调变化触发重连

  useEffect(() => {
    if (!enabled) {
      chatSocket.disconnect();
      return;
    }
    const unsubscribe = chatSocket.subscribe((msg) => onMsgRef.current(msg));
    chatSocket.connect();
    return unsubscribe;
  }, [enabled]);

  return {};
}
