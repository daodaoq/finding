package com.finding.service;

import com.finding.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息推送服务 —— 封装 WebSocket 推送逻辑。
 * 参考 MallChat 的 PushService 设计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final WebSocketServer webSocketServer;

    /** 推送给所有在线用户 */
    public void sendPushMsg(Object message) {
        webSocketServer.sendToAllOnline(message);
    }

    /** 推送给指定用户列表 */
    public void sendPushMsg(Object message, List<Long> uidList) {
        uidList.forEach(uid -> webSocketServer.sendToUser(uid, message));
    }

    /** 推送给单个用户 */
    public void sendPushMsg(Object message, Long uid) {
        webSocketServer.sendToUser(uid, message);
    }
}
