package com.finding.mate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mate_invitation")
public class MateInvitation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String category;
    private String title;
    private String description;
    private LocalDateTime activityTime;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer isAnonymous;
    private Integer status;         // 0=cancelled, 1=active, 2=closed
    private Integer reviewStatus;   // 0=已发布 1=待审 2=拒绝
    private String reviewReason;
    private Long reviewBy;
    private LocalDateTime reviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
