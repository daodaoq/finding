package com.finding.bridge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信息互换申请 —— fromUserId 向 toUserId 发起互换,status 记录流程状态。
 */
@Data
@TableName("user_info_share")
public class InfoShare {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Integer status;         // 0=pending, 1=approved, 2=rejected

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
