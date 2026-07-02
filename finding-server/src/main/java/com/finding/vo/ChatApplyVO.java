package com.finding.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatApplyVO {

    private Long id;
    private Long fromUserId;
    private String fromUserNickname;
    private String fromUserAvatar;
    private Long toUserId;
    private String toUserNickname;
    private String toUserAvatar;
    private Integer status;         // 0=pending, 1=approved, 2=rejected
    private String statusDesc;
    private String remark;
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;
    private Long conversationId;    // set when approved (conversation created)
}
