package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 会话 —— 用户维度的房间订阅，记录每个用户在房间的已读/活跃状态 */
@Data
@TableName("contact")
public class Contact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uid;
    private Long roomId;
    private LocalDateTime readTime;
    private LocalDateTime activeTime;
    private Long lastMsgId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
