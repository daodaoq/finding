package com.finding.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户投诉记录 —— 从聊天里投诉对方。
 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long targetUserId;
    private Long roomId;
    private String targetType;      // message/post/comment/user/resume
    private Long targetId;
    private String contentSnapshot; // 被投诉内容快照
    private String reason;
    private Integer status;         // 0=待处理 1=已处理 2=驳回

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
