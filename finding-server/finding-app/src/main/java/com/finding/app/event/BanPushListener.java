package com.finding.app.event;

import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import com.finding.user.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 监听封禁事件 → 通过 WebSocket 实时推送给被封禁用户,前端弹「账号已被封禁」提示框。
 */
@Component
@RequiredArgsConstructor
public class BanPushListener {

    private final WebSocketServer webSocketServer;

    @EventListener
    public void onUserBanned(UserBannedEvent event) {
        WsMessage ws = new WsMessage();
        ws.setType("ban");
        ws.setTitle("账号已被封禁");
        ws.setToUserId(event.getUserId());
        ws.setContent(buildText(event));
        ws.setTimestamp(System.currentTimeMillis());
        webSocketServer.sendToUser(event.getUserId(), ws);
    }

    private String buildText(UserBannedEvent event) {
        StringBuilder sb = new StringBuilder();
        if (event.getBannedUntil() != null) {
            String until = event.getBannedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            sb.append("你已被封禁至 ").append(until).append("。");
        } else {
            sb.append("你已被永久封禁。");
        }
        if (event.getReason() != null && !event.getReason().isEmpty()) {
            sb.append("\n封禁原因：").append(event.getReason());
        }
        return sb.toString();
    }
}
