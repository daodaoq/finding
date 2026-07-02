package com.finding.vo;

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
    private Integer isRead;
    private LocalDateTime createdAt;
}
