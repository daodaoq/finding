package com.finding.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finding.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 核心处理器 —— 管理连接、收发消息。
 * 参考 MallChat 的 NettyWebSocketServerHandler 架构，用 Spring WebSocket 实现。
 *
 * 核心数据结构：
 * - ONLINE_MAP: Channel → userId，所有在线连接
 * - USER_CHANNELS: userId → Set<Channel>，一个用户可能有多个连接（多端）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketServer extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    /** 所有在线连接: WebSocketSession → 用户ID */
    public static final ConcurrentHashMap<WebSocketSession, Long> ONLINE_MAP = new ConcurrentHashMap<>();

    /** 用户 → 连接集合（支持多端在线） */
    public static final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> USER_CHANNELS = new ConcurrentHashMap<>();

    // ── 连接建立 ──

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从 URL 参数中提取 token
        String token = extractToken(session);
        if (token != null && jwtTokenProvider.validateAccessToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            if (userId != null) {
                ONLINE_MAP.put(session, userId);
                USER_CHANNELS.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
                log.info("WebSocket 连接成功: userId={}, sessionId={}", userId, session.getId());
                return;
            }
        }
        // 认证失败，关闭连接
        log.warn("WebSocket 认证失败，关闭连接: {}", session.getId());
        try { session.close(); } catch (IOException ignored) {}
    }

    // ── 接收消息 ──

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = ONLINE_MAP.get(session);
        if (userId == null) return;

        try {
            WsMessage wsMsg = objectMapper.readValue(message.getPayload(), WsMessage.class);
            wsMsg.setFromUserId(userId);

            // 根据消息类型分发
            if ("chat".equals(wsMsg.getType())) {
                // 私聊消息：推送给接收者
                sendToUser(wsMsg.getToUserId(), wsMsg);
            } else if ("heartbeat".equals(wsMsg.getType())) {
                // 心跳：回复 pong
                WsMessage pong = new WsMessage();
                pong.setType("pong");
                sendToSession(session, pong);
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败", e);
        }
    }

    // ── 连接断开 ──

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = ONLINE_MAP.remove(session);
        if (userId != null) {
            CopyOnWriteArraySet<WebSocketSession> channels = USER_CHANNELS.get(userId);
            if (channels != null) {
                channels.remove(session);
                if (channels.isEmpty()) {
                    USER_CHANNELS.remove(userId);
                }
            }
            log.info("WebSocket 断开: userId={}, sessionId={}", userId, session.getId());
        }
    }

    // ── 发送方法 ──

    /** 广播消息给所有在线用户（热门群聊用） */
    public void sendToAllOnline(Object message) {
        String json = toJson(message);
        ONLINE_MAP.forEach((session, uid) -> sendToSession(session, json));
    }

    /** 发送消息给指定用户（所有端） */
    public void sendToUser(Long userId, Object message) {
        CopyOnWriteArraySet<WebSocketSession> channels = USER_CHANNELS.get(userId);
        if (channels == null || channels.isEmpty()) return;
        String json = toJson(message);
        channels.forEach(s -> sendToSession(s, json));
    }

    /** 发送消息给单个连接 */
    private void sendToSession(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                String json = message instanceof String ? (String) message : toJson(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送 WebSocket 消息失败", e);
        }
    }

    /** 判断用户是否在线 */
    public boolean isOnline(Long userId) {
        CopyOnWriteArraySet<WebSocketSession> channels = USER_CHANNELS.get(userId);
        return channels != null && !channels.isEmpty();
    }

    // ── 工具方法 ──

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
