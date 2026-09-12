package com.finding.framework.service.impl;

import com.finding.framework.service.PushService;
import com.finding.framework.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private final WebSocketServer webSocketServer;

    @Override
    public void sendPushMsg(Object message) {
        webSocketServer.sendToAllOnline(message);
    }

    @Override
    public void sendPushMsg(Object message, List<Long> uidList) {
        uidList.forEach(uid -> webSocketServer.sendToUser(uid, message));
    }

    @Override
    public void sendPushMsg(Object message, Long uid) {
        webSocketServer.sendToUser(uid, message);
    }
}
