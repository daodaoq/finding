package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 群成员 */
@Data
@TableName("group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long uid;
    private Integer role;   // 0=成员 1=管理员 2=群主

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
