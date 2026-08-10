package com.finding.group.entity;

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
    private Integer isRecalled; // 0=否 1=已撤回

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
