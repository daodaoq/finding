package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 单聊房间关联 —— 记录两个用户与房间的绑定关系 */
@Data
@TableName("room_friend")
public class RoomFriend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long uid1;
    private Long uid2;
    private String roomKey;     // 格式: uid1_uid2 (uid1 < uid2)
    private Integer status;     // 0=拉黑 1=正常

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
