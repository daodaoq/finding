package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_chat")
public class GroupChat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private Long ownerId;
    private Integer memberCount;
    private String announcement;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
