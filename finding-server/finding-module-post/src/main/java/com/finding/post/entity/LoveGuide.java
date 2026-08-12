package com.finding.post.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("love_guide")
public class LoveGuide {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String title;
    private String subtitle;
    private String content;
    private String category;
    /** 0=pending, 1=approved, 2=rejected */ private Integer reviewStatus;
    private String reviewReason;
    private Long reviewBy;
    private LocalDateTime reviewTime;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
