package com.finding.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupChatVO {
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private Integer memberCount;
    private String announcement;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    /** 成员列表（详情场景用） */
    private List<GroupMemberVO> members;
    /** 最近活跃用户 ID 列表（列表场景用） */
    private List<Long> recentUserIds;

    @Data
    public static class GroupMemberVO {
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer role; // 0=member, 1=admin, 2=owner
    }
}
