package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 群聊房间关联 */
@Data
@TableName("room_group")
public class RoomGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String name;
    private String avatar;
    private Long ownerUid;
    private String announcement;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
