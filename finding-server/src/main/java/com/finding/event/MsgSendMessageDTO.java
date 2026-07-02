package com.finding.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * MQ 消息体 —— 聊天消息发送后的路由信息。
 * 参考 MallChat 的 MsgSendMessageDTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MsgSendMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** private_chat 消息 ID */
    private Long msgId;
}
