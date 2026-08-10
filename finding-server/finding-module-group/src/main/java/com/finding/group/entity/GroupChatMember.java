package com.finding.group.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_chat_member")
public class GroupChatMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role; // 0=member, 1=admin, 2=owner
    private Long lastReadMsgId; // 该群最后已读的消息ID(0=未读任何)

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
