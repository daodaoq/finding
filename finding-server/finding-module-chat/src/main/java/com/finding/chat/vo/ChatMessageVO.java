package com.finding.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {

    private Long id;
    private Long roomId;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String messageType;     // text / image
    private Integer isRecalled;
    private Integer isRead;
    /** 回复/引用:被回复消息 ID(前端在本地消息中查原文渲染,不在历史接口逐条反查避免 N+1) */
    private Long parentMessageId;
    private LocalDateTime createdAt;
}
