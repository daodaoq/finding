package com.finding.chat.event;

import com.finding.chat.entity.ChatOutbox;
import com.finding.framework.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 聊天消息 Outbox 发布任务 —— 扫描待发布记录,调用 RabbitMQ 发布并等待 broker 确认后标记完成。
 *
 * 可靠性设计:
 * - 发布失败按指数退避设置 next_retry_at,避免 broker 故障时每秒空转
 * - 达到最大重试次数标记为死信(status=2)并输出 error 日志,供人工核查
 * - 轮询使用 JdbcTemplate 直查(而非 MyBatis Mapper),避免定时轮询 SQL 被 StdOutImpl 打印刷屏
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatOutboxPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    static final int BATCH = 50;
    /** 最大重试次数:失败达到该次数后标记死信 */
    static final int MAX_RETRY = 5;
    /** 指数退避基数(秒):2s * 2^retryCount,封顶 60s */
    static final long BACKOFF_BASE_SECONDS = 2;
    static final long CONFIRM_TIMEOUT_MS = 2000;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        // 只取"当前可重试"的记录:未失败过或已过 next_retry_at
        List<ChatOutbox> pending = jdbcTemplate.query(
                "SELECT id, message_id, status, retry_count, last_error, created_at, published_at, next_retry_at "
                        + "FROM chat_outbox WHERE status = ? AND retry_count < ? "
                        + "AND (next_retry_at IS NULL OR next_retry_at <= NOW()) ORDER BY id ASC LIMIT ?",
                (rs, i) -> {
                    ChatOutbox o = new ChatOutbox();
                    o.setId(rs.getLong("id"));
                    o.setMessageId(rs.getLong("message_id"));
                    o.setStatus(rs.getInt("status"));
                    o.setRetryCount(rs.getInt("retry_count"));
                    o.setLastError(rs.getString("last_error"));
                    o.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    o.setPublishedAt(rs.getObject("published_at", LocalDateTime.class));
                    o.setNextRetryAt(rs.getObject("next_retry_at", LocalDateTime.class));
                    return o;
                },
                ChatOutbox.STATUS_PENDING, MAX_RETRY, BATCH);
        if (pending.isEmpty()) return;

        for (ChatOutbox outbox : pending) {
            publishAndApply(outbox);
        }
    }

    /** 发布单条并应用结果(成功/重试/死信);抽出便于单元测试 */
    void publishAndApply(ChatOutbox outbox) {
        String error = publish(outbox);
        applyPublishResult(outbox, error);
    }

    /** 发布并等待 broker 确认;成功返回 null,失败返回错误摘要 */
    private String publish(ChatOutbox outbox) {
        try {
            CorrelationData cd = new CorrelationData(String.valueOf(outbox.getId()));
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_SEND_MSG,
                    new MsgSendMessageDTO(outbox.getMessageId()), cd);
            if (cd.getFuture() != null) {
                Confirm confirm = cd.getFuture().get(CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                return confirm != null && confirm.isAck() ? null : "broker nack";
            }
            // 未开启 publisher confirms,视为成功(仍会被 broker 接收)
            return null;
        } catch (Exception e) {
            log.warn("Outbox 发布失败, 将重试: outboxId={}, msgId={}, err={}",
                    outbox.getId(), outbox.getMessageId(), e.getMessage());
            String msg = e.getMessage();
            return msg != null && msg.length() > 200 ? msg.substring(0, 200) : String.valueOf(msg);
        }
    }

    /**
     * 依据发布结果更新 outbox 状态:
     * - 成功 → status=1,记录发布时间
     * - 失败未达上限 → retry_count+1,按指数退避设置 next_retry_at
     * - 失败达上限 → status=2 死信 + 告警日志
     */
    void applyPublishResult(ChatOutbox outbox, String error) {
        long id = outbox.getId();
        if (error == null) {
            jdbcTemplate.update(
                    "UPDATE chat_outbox SET status = ?, published_at = NOW(), last_error = NULL WHERE id = ?",
                    ChatOutbox.STATUS_PUBLISHED, id);
            return;
        }
        if (outbox.getRetryCount() + 1 >= MAX_RETRY) {
            jdbcTemplate.update(
                    "UPDATE chat_outbox SET status = ?, last_error = ? WHERE id = ?",
                    ChatOutbox.STATUS_DEAD, error, id);
            log.error("Outbox 达到最大重试次数,标记死信,请人工核查: outboxId={}, msgId={}, err={}",
                    id, outbox.getMessageId(), error);
            return;
        }
        LocalDateTime next = LocalDateTime.now().plusSeconds(
                Math.min(BACKOFF_BASE_SECONDS << Math.min(outbox.getRetryCount(), 5), 60));
        jdbcTemplate.update(
                "UPDATE chat_outbox SET retry_count = retry_count + 1, last_error = ?, next_retry_at = ? WHERE id = ?",
                error, next, id);
    }
}
