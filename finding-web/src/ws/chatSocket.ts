/**
 * 全局唯一 WebSocket 连接控制器 —— 单个标签页只保持一条 /ws/chat 连接。
 *
 * 修复要点:
 * - 多页面(MainLayout / 聊天 / 群聊 / 消息)共用单例,不再各自建连
 * - 指数退避重连(1s→30s 封顶),组件卸载/路由切换只退订、不断开全局连接
 * - 从未成功建立连接即关闭(token 无效/握手被拒)→ 停止重连,避免无限循环
 * - 窗口重新聚焦 / 网络恢复时补连,清除所有定时器
 */

export interface WsMessage {
  type: string;
  action?: string;
  title?: string;
  fromUserId: number;
  fromUserNickname?: string;
  fromUserAvatar?: string;
  toUserId: number;
  conversationId: number;
  content: string;
  messageType: string;
  messageId: number;
  /** 回复/引用:被回复消息 ID */
  parentMessageId?: number;
  muted?: boolean;
  timestamp: number;
}

type WsHandler = (msg: WsMessage) => void;

const BACKOFF_MS = [1000, 2000, 4000, 8000, 16000, 30000];

class ChatSocket {
  private ws: WebSocket | null = null;
  private handlers = new Set<WsHandler>();
  private connectHandlers = new Set<() => void>();
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private retryCount = 0;
  private manualClose = false;
  private everOpened = false;

  constructor() {
    window.addEventListener('focus', this.tryReconnect);
    window.addEventListener('online', this.tryReconnect);
  }

  /** 建立连接(幂等):已有连接则忽略;未登录则跳过 */
  connect() {
    this.manualClose = false;
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) return;
    if (!localStorage.getItem('accessToken')) return;
    this.open();
  }

  /** 主动断开(退出登录/注销):清除定时器,不再重连 */
  disconnect() {
    this.manualClose = true;
    this.clearTimers();
    this.ws?.close();
    this.ws = null;
    this.retryCount = 0;
    this.everOpened = false;
  }

  /** 订阅消息,返回取消订阅函数(组件卸载时调用,不影响全局连接) */
  subscribe(handler: WsHandler) {
    this.handlers.add(handler);
    return () => { this.handlers.delete(handler); };
  }

  /** 订阅"连接建立"(首次连接或断线重连成功)事件,用于断线补偿补拉缺失消息 */
  onReconnect(handler: () => void) {
    this.connectHandlers.add(handler);
    return () => { this.connectHandlers.delete(handler); };
  }

  /** 当前连接是否已打开 */
  isOpen() {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  /** 发送输入状态轻量事件(仅 typing,不透传聊天内容;服务端仅透传 conversationId) */
  sendTyping(conversationId: number, toUserId: number) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type: 'typing', conversationId, toUserId }));
    }
  }

  private open() {
    const token = localStorage.getItem('accessToken');
    if (!token) return;
    const url = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/chat?token=${token}`;
    const ws = new WebSocket(url);
    this.ws = ws;

    ws.onopen = () => {
      this.everOpened = true;
      this.retryCount = 0;
      this.connectHandlers.forEach((h) => h());
      if (this.heartbeatTimer) clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify({ type: 'heartbeat' }));
      }, 30000);
    };

    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data) as WsMessage;
        this.handlers.forEach((h) => h(msg));
      } catch { /* 忽略无法解析的消息 */ }
    };

    ws.onclose = () => {
      if (this.heartbeatTimer) { clearInterval(this.heartbeatTimer); this.heartbeatTimer = null; }
      if (this.ws === ws) this.ws = null;
      if (this.manualClose) return;
      // 从未建立连接即关闭(认证失败/握手被拒),或 token 已失效/登出 → 停止重连
      if (!this.everOpened || !this.hasValidToken()) {
        console.warn('[chatSocket] WebSocket 认证失败或 token 失效,停止重连');
        return;
      }
      const delay = BACKOFF_MS[Math.min(this.retryCount, BACKOFF_MS.length - 1)];
      this.retryCount += 1;
      if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
      this.reconnectTimer = setTimeout(() => this.open(), delay);
    };

    ws.onerror = () => { ws.close(); };
  }

  /** 焦点恢复/网络恢复时补连(覆盖"首次连接时服务端暂不可用"的情况) */
  private tryReconnect = () => {
    if (this.manualClose || this.ws || !this.hasValidToken()) return;
    this.retryCount = 0;
    this.open();
  };

  private hasValidToken(): boolean {
    const token = localStorage.getItem('accessToken');
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private clearTimers() {
    if (this.reconnectTimer) { clearTimeout(this.reconnectTimer); this.reconnectTimer = null; }
    if (this.heartbeatTimer) { clearInterval(this.heartbeatTimer); this.heartbeatTimer = null; }
  }
}

export const chatSocket = new ChatSocket();
