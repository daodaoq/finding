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
 * 轮询使用 JdbcTemplate 直查(而非 MyBatis Mapper),避免定时轮询的 SQL 被
 * MyBatis StdOutImpl 打印刷屏;RabbitMQ 暂不可用时发布失败,记录留待下一轮重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatOutboxPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final int BATCH = 50;
    private static final int MAX_RETRY = 5;
    private static final long CONFIRM_TIMEOUT_MS = 2000;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<ChatOutbox> pending = jdbcTemplate.query(
                "SELECT id, message_id, status, retry_count, last_error, created_at, published_at "
                        + "FROM chat_outbox WHERE status = 0 AND retry_count < ? ORDER BY id ASC LIMIT ?",
                (rs, i) -> {
                    ChatOutbox o = new ChatOutbox();
                    o.setId(rs.getLong("id"));
                    o.setMessageId(rs.getLong("message_id"));
                    o.setStatus(rs.getInt("status"));
                    o.setRetryCount(rs.getInt("retry_count"));
                    o.setLastError(rs.getString("last_error"));
                    o.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    o.setPublishedAt(rs.getObject("published_at", LocalDateTime.class));
                    return o;
                },
                MAX_RETRY, BATCH);
        if (pending.isEmpty()) return;

        for (ChatOutbox outbox : pending) {
            String err = publish(outbox);
            if (err == null) {
                jdbcTemplate.update(
                        "UPDATE chat_outbox SET status = 1, published_at = NOW(), last_error = NULL WHERE id = ?",
                        outbox.getId());
            } else {
                jdbcTemplate.update(
                        "UPDATE chat_outbox SET retry_count = retry_count + 1, last_error = ? WHERE id = ?",
                        err, outbox.getId());
            }
        }
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
}
