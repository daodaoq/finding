package com.finding.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    private Long id;                // room.id (conversation/session ID)
    private Long roomId;            // same as id, for frontend message query
    private Long targetUserId;
    private String targetNickname;
    private String targetAvatar;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
}
