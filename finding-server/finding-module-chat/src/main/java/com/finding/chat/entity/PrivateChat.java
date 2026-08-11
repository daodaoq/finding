package com.finding.chat.entity;

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
    /** 客户端幂等 ID(senderId + clientMessageId 唯一,弱网重试不重复落库) */
    private String clientMessageId;
    /** 被回复消息 ID(回复/引用) */
    private Long parentMessageId;
    private Integer isRecalled;     // 0=否 1=已撤回
    private Integer isRead;
    /** uid1(较小者)是否已单侧清空该消息 */
    private Integer uid1Hidden;
    /** uid2(较大者)是否已单侧清空该消息 */
    private Integer uid2Hidden;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
