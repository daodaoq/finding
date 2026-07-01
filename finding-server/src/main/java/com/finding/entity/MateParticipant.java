package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mate_participant")
public class MateParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long invitationId;
    private Long userId;
    private Integer status;         // 0=pending, 1=accepted, 2=rejected
    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
