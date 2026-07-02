package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_message")
public class GroupMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long fromUserId;
    private String content;
    private String messageType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
