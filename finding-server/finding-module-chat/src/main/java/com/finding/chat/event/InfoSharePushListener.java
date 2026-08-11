package com.finding.chat.event;

import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 信息互换 WS 推送监听 —— 事务提交后实时推送给接收方。
 * fallbackExecution=true: 无事务上下文时立即投递。
 */
@Component
@RequiredArgsConstructor
public class InfoSharePushListener {

    private final WebSocketServer webSocketServer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInfoSharePush(InfoSharePushEvent event) {
        if (!webSocketServer.isOnline(event.toUserId())) {
            return;
        }
        WsMessage wsMsg = new WsMessage();
        wsMsg.setType("info_share");
        wsMsg.setAction(event.action());
        wsMsg.setFromUserId(event.fromUserId());
        wsMsg.setToUserId(event.toUserId());
        // WS content 只放昵称,由前端弹窗拼完整文案
        wsMsg.setContent(event.content());
        wsMsg.setMessageId(event.shareId());
        wsMsg.setTimestamp(System.currentTimeMillis());
        webSocketServer.sendToUser(event.toUserId(), wsMsg);
    }
}
