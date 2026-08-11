package com.finding.mate.vo;

import com.finding.user.vo.UserVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MateVO {

    private Long id;
    private Long userId;
    private String category;
    private String categoryDesc;
    private String title;
    private String description;
    private LocalDateTime activityTime;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer isAnonymous;
    private Integer status;
    private Integer reviewStatus;
    private String reviewReason;

    // Author info (masked if isAnonymous=1)
    private UserVO author;

    // Computed fields
    private Double distanceKm;
    private Boolean hasJoined;
    private Boolean isFull;
    /** 当前用户的报名状态:0=待审核 1=已通过 2=已拒绝 3=已退出 4=候补;未报名为 null */
    private Integer myApplicationStatus;
    /** 是否已过期(activityTime 已过) */
    private Boolean isExpired;
    /** 剩余名额(max - current) */
    private Integer remainingSlots;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
