package com.finding.chat.event;

import com.finding.chat.entity.ChatOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * P1-3 Outbox 可靠性:成功标记发布 / 失败退避重试 / 达上限死信 / broker 不可用时走重试。
 */
@ExtendWith(MockitoExtension.class)
class ChatOutboxPublisherTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private ChatOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ChatOutboxPublisher(jdbcTemplate, rabbitTemplate);
    }

    private ChatOutbox outbox(int retryCount) {
        ChatOutbox o = new ChatOutbox();
        o.setId(100L);
        o.setMessageId(200L);
        o.setStatus(ChatOutbox.STATUS_PENDING);
        o.setRetryCount(retryCount);
        return o;
    }

    @Test
    void publishSuccess_marksPublished() {
        publisher.applyPublishResult(outbox(0), null);

        verify(jdbcTemplate).update(
                eq("UPDATE chat_outbox SET status = ?, published_at = NOW(), last_error = NULL WHERE id = ?"),
                eq(ChatOutbox.STATUS_PUBLISHED), eq(100L));
    }

    @Test
    void publishFailure_bumpsRetryAndSetsBackoff() {
        publisher.applyPublishResult(outbox(0), "broker down");

        verify(jdbcTemplate).update(
                eq("UPDATE chat_outbox SET retry_count = retry_count + 1, last_error = ?, next_retry_at = ? WHERE id = ?"),
                eq("broker down"), any(LocalDateTime.class), eq(100L));
    }

    @Test
    void publishFailure_reachingMaxRetry_marksDead() {
        publisher.applyPublishResult(outbox(ChatOutboxPublisher.MAX_RETRY - 1), "persistent failure");

        verify(jdbcTemplate).update(
                eq("UPDATE chat_outbox SET status = ?, last_error = ? WHERE id = ?"),
                eq(ChatOutbox.STATUS_DEAD), eq("persistent failure"), eq(100L));
    }

    @Test
    void publishAndApply_rabbitUnavailable_bumpsRetry() {
        // broker 不可用 → convertAndSend 抛异常 → 走失败路径:重试 + 退避,不误判为死信
        doThrow(new RuntimeException("Connection refused")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        publisher.publishAndApply(outbox(0));

        verify(jdbcTemplate).update(
                eq("UPDATE chat_outbox SET retry_count = retry_count + 1, last_error = ?, next_retry_at = ? WHERE id = ?"),
                contains("Connection refused"), any(LocalDateTime.class), eq(100L));
    }
}
