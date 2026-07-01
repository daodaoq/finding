package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;        // NULL for system
    private Long toUserId;
    private String type;            // like/comment/follow/mate_request/mate_accepted/mate_rejected/system
    private String content;
    private Long relatedId;         // post_id or invitation_id
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
