package com.finding.chat.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.chat.entity.ChatOutbox;
import com.finding.chat.mapper.ChatOutboxMapper;
import com.finding.framework.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 聊天消息 Outbox 发布任务 —— 扫描待发布记录,调用 RabbitMQ 发布并等待 broker 确认后标记完成。
 * RabbitMQ 暂不可用时发布失败,记录留待下一轮重试;确认成功后才置为已发布,保证事件不丢失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatOutboxPublisher {

    private final ChatOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;

    private static final int BATCH = 50;
    private static final int MAX_RETRY = 5;
    private static final long CONFIRM_TIMEOUT_MS = 2000;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<ChatOutbox> pending = outboxMapper.selectList(new LambdaQueryWrapper<ChatOutbox>()
                .eq(ChatOutbox::getStatus, 0)
                .lt(ChatOutbox::getRetryCount, MAX_RETRY)
                .orderByAsc(ChatOutbox::getId)
                .last("LIMIT " + BATCH));
        if (pending.isEmpty()) return;

        for (ChatOutbox outbox : pending) {
            boolean acked = publish(outbox);
            if (acked) {
                outbox.setStatus(1);
                outbox.setPublishedAt(LocalDateTime.now());
                outbox.setLastError(null);
                outboxMapper.updateById(outbox);
            } else {
                outbox.setRetryCount(outbox.getRetryCount() == null ? 1 : outbox.getRetryCount() + 1);
                outboxMapper.updateById(outbox);
            }
        }
    }

    private boolean publish(ChatOutbox outbox) {
        try {
            CorrelationData cd = new CorrelationData(String.valueOf(outbox.getId()));
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_SEND_MSG,
                    new MsgSendMessageDTO(outbox.getMessageId()), cd);
            if (cd.getFuture() != null) {
                Confirm confirm = cd.getFuture().get(CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                return confirm != null && confirm.isAck();
            }
            // 未开启 publisher confirms,视为成功(仍会被 broker 接收)
            return true;
        } catch (Exception e) {
            log.warn("Outbox 发布失败, 将重试: outboxId={}, msgId={}, err={}",
                    outbox.getId(), outbox.getMessageId(), e.getMessage());
            return false;
        }
    }
}
