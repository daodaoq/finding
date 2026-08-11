package com.finding.app.listener;

import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import com.finding.message.event.NewNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 新站内通知监听 —— 事务提交后 WS 实时推送给目标用户,用户端刷新未读角标。
 *
 * <p>站内信写入(notify)与业务写入处于同一事务,具备持久性;WS 是外部副作用,
 * 必须等到事务提交后才推送,避免事务回滚时产生"已推送但库中无记录"的不一致。
 * fallbackExecution=true: 无事务上下文(如纯查询场景)时立即投递。</p>
 */
@Component
@RequiredArgsConstructor
public class NewNotificationListener {

    private final WebSocketServer webSocketServer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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
