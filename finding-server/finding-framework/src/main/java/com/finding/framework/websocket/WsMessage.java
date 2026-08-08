package com.finding.framework.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WsMessage {

    /** 消息类型: chat / heartbeat / pong / system / info_share */
    private String type;

    /** 业务动作(如 info_share: request / approved / rejected) */
    private String action;

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
