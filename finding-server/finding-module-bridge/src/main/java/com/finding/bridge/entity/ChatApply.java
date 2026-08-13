package com.finding.bridge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_apply")
public class ChatApply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Integer status;         // 0=pending, 1=approved, 2=rejected, 3=cancelled, 4=expired
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime applyTime;

    private LocalDateTime handleTime;

    /** 处理人用户ID(撤回/审批/系统过期) */
    private Long handleBy;
}
