package com.finding.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 陌生人打招呼消息 —— 未建立正式会话前,同一对用户仅允许一条。
 * 接收方确认后转为正式会话,该消息迁移进 private_chat。
 */
@Data
@TableName("stranger_message")
public class StrangerMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String messageType;

    /** 0=待确认 1=已确认(已转为正式会话) */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
