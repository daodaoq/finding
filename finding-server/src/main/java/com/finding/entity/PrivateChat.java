package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_chat")
public class PrivateChat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;    // deprecated, use roomId
    private Long roomId;            // FK to room.id (MallChat model)
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String messageType;     // text / image
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
