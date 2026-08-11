package com.finding.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息发送 Outbox —— 事务内与 private_chat 一起落库,后台任务再发布 MQ。
 * 保证 RabbitMQ 暂不可用时消息推送事件不丢失,恢复后由定时任务补发。
 */
@Data
@TableName("chat_outbox")
public class ChatOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 private_chat.id */
    private Long messageId;

    /** 0=待发布 1=已发布 */
    private Integer status;

    private Integer retryCount;

    private String lastError;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;
}
