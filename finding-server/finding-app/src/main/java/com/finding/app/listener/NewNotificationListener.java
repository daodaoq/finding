package com.finding.app.listener;

import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import com.finding.message.event.NewNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 新站内通知监听 —— 收到后 WS 实时推送给目标用户,用户端刷新未读角标。
 */
@Component
@RequiredArgsConstructor
public class NewNotificationListener {

    private final WebSocketServer webSocketServer;

    @EventListener
    public void onNewNotification(NewNotificationEvent event) {
        if (event.toUserId() == null) {
            return;
        }
        WsMessage ws = new WsMessage();
        ws.setType("new_notification");
        ws.setToUserId(event.toUserId());
        ws.setTimestamp(System.currentTimeMillis());
        webSocketServer.sendToUser(event.toUserId(), ws);
    }
}
