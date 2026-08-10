package com.finding.app.entity;

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
    private Integer type;   // 1=普通公告(弹窗) 2=永久展示(顶部横条)
    private Integer status; // 1=展示中 0=已下架
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
