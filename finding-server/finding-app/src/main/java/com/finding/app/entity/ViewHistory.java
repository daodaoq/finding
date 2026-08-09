package com.finding.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 浏览记录 —— 每用户每目标一条,重复浏览刷新时间。
 */
@Data
@TableName("view_history")
public class ViewHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType;      // post / user
    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
