package com.finding.framework.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WsMessage {

    /** 消息类型: chat / heartbeat / pong / system / info_share / system_announcement */
    private String type;

    /** 业务动作(如 info_share: request / approved / rejected) */
    private String action;

    /** 业务标题(如系统公告的标题) */
    private String title;

    /** 发送者ID（服务端填充） */
    private Long fromUserId;

    /** 发送者昵称(群聊实时推送用) */
    private String fromUserNickname;

    /** 发送者头像(群聊实时推送用) */
    private String fromUserAvatar;

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

    /** 接收方是否对该会话免打扰(推送仍会发送,前端据此抑制声音/弹窗,不影响数据同步) */
    private Boolean muted;

    /** 时间戳 */
    private Long timestamp;
}
