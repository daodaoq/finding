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
 * WebSocket 连接 Hook —— 自动连接、心跳保活、消息回调。
 */
export function useWebSocket(onMessage: (msg: WsMessage) => void) {
  const wsRef = useRef<WebSocket | null>(null);
  const heartbeatRef = useRef<ReturnType<typeof setInterval>>();

  const connect = useCallback(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const ws = new WebSocket(`${protocol}//${host}/ws/chat?token=${token}`);

    ws.onopen = () => {
      console.log('WebSocket 已连接');
      // 心跳：每30秒发送一次
      heartbeatRef.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'heartbeat' }));
        }
      }, 30000);
    };

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data);
        onMessage(msg);
      } catch (e) {
        console.error('解析 WebSocket 消息失败', e);
      }
    };

    ws.onclose = () => {
      console.log('WebSocket 断开，3秒后重连');
      if (heartbeatRef.current) clearInterval(heartbeatRef.current);
      setTimeout(connect, 3000);
    };

    ws.onerror = (e) => {
      console.error('WebSocket 错误', e);
      ws.close();
    };

    wsRef.current = ws;
  }, [onMessage]);

  useEffect(() => {
    connect();
    return () => {
      if (heartbeatRef.current) clearInterval(heartbeatRef.current);
      wsRef.current?.close();
    };
  }, [connect]);

  /** 发送消息 */
  const sendMessage = useCallback((msg: Partial<WsMessage>) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(msg));
    }
  }, []);

  return { sendMessage };
}
