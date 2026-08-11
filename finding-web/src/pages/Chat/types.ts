import type { ChatMessageDTO } from '../../types/message';
import type { WsMessage } from '../../ws/chatSocket';
import type { MessageLike } from './components/MessageList';

/** 聊天消息(本地展示模型,由后端 DTO / WS 消息 / 本地临时消息转换而来) */
export interface ChatMessage extends MessageLike {
  toUserId: number;
  isRead: number;
  /** 客户端幂等 ID(弱网重试复用,服务端据此去重) */
  clientMessageId?: string;
  /** 发送状态(仅自己刚发送/失败的消息) */
  sendState?: 'sending' | 'sent' | 'failed';
}

/** 后端消息记录 → 本地消息结构 */
export function toMsg(r: ChatMessageDTO): ChatMessage {
  return {
    id: r.id,
    fromUserId: r.fromUserId,
    toUserId: r.toUserId,
    content: r.content,
    messageType: r.messageType || 'text',
    isRecalled: r.isRecalled,
    isRead: r.isRead,
    parentMessageId: r.parentMessageId,
    createdAt: r.createdAt,
  };
}

/** WebSocket 实时消息 → 本地消息结构(接收方消息 / 多端同步) */
export function wsToChatMessage(ws: WsMessage): ChatMessage {
  return {
    id: ws.messageId,
    fromUserId: ws.fromUserId,
    toUserId: ws.toUserId,
    content: ws.content,
    messageType: ws.messageType || 'text',
    isRead: 0,
    parentMessageId: ws.parentMessageId,
    createdAt: new Date().toISOString(),
  };
}
