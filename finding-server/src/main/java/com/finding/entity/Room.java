package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 房间 —— 聊天容器，封装群聊和单聊 */
@Data
@TableName("room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer type;       // 1=单聊 2=群聊 3=全员群
    private Integer hotFlag;    // 0=普通 1=热门群
    private LocalDateTime activeTime;
    private Long lastMsgId;
    private String extJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public boolean isRoomFriend() { return type != null && type == 1; }
    public boolean isRoomGroup() { return type != null && type == 2; }
    public boolean isHotRoom() { return hotFlag != null && hotFlag == 1; }
}
