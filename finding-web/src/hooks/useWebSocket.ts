import { useEffect, useRef, useCallback } from 'react';

interface WsMessage {
  type: string;
  fromUserId: number;
  toUserId: number;
  conversationId: number;
  content: string;
  messageType: string;
  messageId: number;
  timestamp: number;
}

/**
 * WebSocket 连接 Hook —— 稳定连接、心跳保活。
 * 用 ref 存储回调避免因回调变化导致反复重连。
 */
export function useWebSocket(onMessage: (msg: WsMessage) => void) {
  const wsRef = useRef<WebSocket | null>(null);
  const heartbeatRef = useRef<ReturnType<typeof setInterval>>();
  const onMsgRef = useRef(onMessage);
  onMsgRef.current = onMessage; // 始终指向最新回调

  const connect = useCallback(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    // 避免重复连接
    if (wsRef.current?.readyState === WebSocket.OPEN ||
        wsRef.current?.readyState === WebSocket.CONNECTING) {
      return;
    }

    const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/chat?token=${token}`;
    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('WebSocket 已连接');
      heartbeatRef.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'heartbeat' }));
        }
      }, 30000);
    };

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data);
        onMsgRef.current(msg); // 通过 ref 调用最新回调
      } catch (e) {
        console.error('解析 WebSocket 消息失败', e);
      }
    };

    ws.onclose = (e) => {
      console.log('WebSocket 断开，3秒后重连, code=', e.code);
      if (heartbeatRef.current) clearInterval(heartbeatRef.current);
      wsRef.current = null;
      setTimeout(connect, 3000);
    };

    ws.onerror = (e) => {
      console.error('WebSocket 错误', e);
      ws.close();
    };

    wsRef.current = ws;
  }, []); // 空依赖，只创建一次

  useEffect(() => {
    connect();
    return () => {
      if (heartbeatRef.current) clearInterval(heartbeatRef.current);
      wsRef.current?.close();
      wsRef.current = null;
    };
  }, [connect]);

  /** 通过 WebSocket 发送消息 */
  const sendMessage = useCallback((msg: Partial<WsMessage>) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(msg));
    }
  }, []);

  return { sendMessage };
}
