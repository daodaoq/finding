package com.finding.chat.entity;

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
    private Integer pinned;         // 0=否 1=置顶
    private Integer muted;          // 0=否 1=消息免打扰
    private Integer hidden;         // 0=否 1=从会话列表隐藏(收到新消息自动恢复)
    private String background;      // 聊天背景(preset key 或图片URL)

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
