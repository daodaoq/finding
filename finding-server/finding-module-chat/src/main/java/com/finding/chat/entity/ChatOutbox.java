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
 * 状态:0=待发布 1=已发布 2=死信(超过最大重试,需人工核查)。
 */
@Data
@TableName("chat_outbox")
public class ChatOutbox {

    /** 待发布 */
    public static final int STATUS_PENDING = 0;
    /** 已发布(收到 broker 确认) */
    public static final int STATUS_PUBLISHED = 1;
    /** 死信:超过最大重试次数,需人工核查 */
    public static final int STATUS_DEAD = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 private_chat.id */
    private Long messageId;

    /** 状态,见 STATUS_* 常量 */
    private Integer status;

    private Integer retryCount;

    /** 最近一次失败原因(200 字内摘要) */
    private String lastError;

    /** 下一次重试时间(指数退避);失败后设置,到点前不再轮询 */
    private LocalDateTime nextRetryAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;
}
