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
    private LocalDateTime createdAt;
}
