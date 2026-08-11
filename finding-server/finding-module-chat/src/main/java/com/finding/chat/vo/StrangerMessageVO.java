package com.finding.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 陌生人打招呼消息视图(带对方资料与方向) */
@Data
public class StrangerMessageVO {

    private Long id;
    /** 对方用户ID(与"我"相对的一方) */
    private Long otherUserId;
    private String otherNickname;
    private String otherAvatar;
    private String content;
    /** sent=我发出的待确认 received=我收到的待确认 */
    private String direction;
    private LocalDateTime createdAt;
}
