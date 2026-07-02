package com.finding.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupMessageVO {
    private Long id;
    private Long groupId;
    private Long fromUserId;
    private String fromUserNickname;
    private String fromUserAvatar;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
}
