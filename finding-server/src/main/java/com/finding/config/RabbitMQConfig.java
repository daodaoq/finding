package com.finding.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 —— 参考 MallChat RocketMQ 双 Topic 架构。
 *
 * 消息流：
 *   生产者 → chat.send.msg (queue) → MsgSendConsumer (更新联系人 + WebSocket 推送)
 *
 * Exchange/Queue 设计：
 *   - Direct Exchange: finding.chat
 *   - Queue: chat.send.msg (routing key: chat.send.msg)
 *
 * MsgSendConsumer 消费后直接调用 PushService 推送给在线用户。
 * 相比 MallChat 的 chat_send_msg → websocket_push 双 Topic，
 * 单 Queue 架构对学生社交平台更简洁，延迟更低。
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "finding.chat";
    public static final String QUEUE_SEND_MSG = "chat.send.msg";
    public static final String RK_SEND_MSG = "chat.send.msg";

    // ── Direct Exchange ──

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    // ── Queues ──

    @Bean
    public Queue sendMsgQueue() {
        return QueueBuilder.durable(QUEUE_SEND_MSG).build();
    }

    // ── Bindings ──

    @Bean
    public Binding sendMsgBinding() {
        return BindingBuilder.bind(sendMsgQueue()).to(chatExchange()).with(RK_SEND_MSG);
    }

    // ── RabbitTemplate ──

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
