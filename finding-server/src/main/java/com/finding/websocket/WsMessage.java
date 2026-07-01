package com.finding.websocket;

import lombok.Data;

/**
 * WebSocket 消息体 —— 客户端和服务端之间的统一消息格式。
 */
@Data
public class WsMessage {

    /** 消息类型: chat / heartbeat / pong / system */
    private String type;

    /** 发送者ID（服务端填充） */
    private Long fromUserId;

    /** 接收者ID */
    private Long toUserId;

    /** 会话ID */
    private Long conversationId;

    /** 消息内容 */
    private String content;

    /** 消息类型: text / image */
    private String messageType = "text";

    /** 消息ID（服务端返回） */
    private Long messageId;

    /** 时间戳 */
    private Long timestamp;
}
