package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_verification")
public class UserVerification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String realName;
    private String studentId;
    private String school;
    private String idCardFront;
    private String idCardBack;
    private String studentCard;
    private Integer status;          // 0=pending, 1=approved, 2=rejected
    private Long reviewerId;
    private String reviewComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
