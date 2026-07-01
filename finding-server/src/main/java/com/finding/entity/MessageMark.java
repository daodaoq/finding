package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 消息标记（点赞/踩/表情反应） */
@Data
@TableName("message_mark")
public class MessageMark {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long msgId;
    private Long uid;
    private Integer markType;   // 1=点赞 2=踩
    private Integer actType;    // 1=标记 2=取消标记
    private Integer status;     // 0=无效 1=有效

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
