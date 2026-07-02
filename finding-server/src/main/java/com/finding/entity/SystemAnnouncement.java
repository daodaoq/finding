package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_announcement")
public class SystemAnnouncement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
