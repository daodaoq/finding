package com.finding.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** 消息发送事件 —— 发布后由监听器异步处理推送和会话更新 */
@Getter
public class MessageSendEvent extends ApplicationEvent {
    private final Long msgId;

    public MessageSendEvent(Object source, Long msgId) {
        super(source);
        this.msgId = msgId;
    }
}
