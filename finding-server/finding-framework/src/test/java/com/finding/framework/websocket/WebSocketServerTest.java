package com.finding.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finding.user.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 安全回归单测 —— P0-2 移除直接转发:
 * 1) 连接鉴权(合法 token 注册 / 非法 token 关闭);
 * 2) 客户端发 type=chat 不再直接转发给接收者,并向发送端回送 chat_rejected;
 * 3) heartbeat 仍回复 pong。
 */
class WebSocketServerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final WebSocketServer server = new WebSocketServer(jwtTokenProvider, objectMapper);

    @BeforeEach
    @AfterEach
    void clearStaticRegistries() {
        WebSocketServer.ONLINE_MAP.clear();
        WebSocketServer.USER_CHANNELS.clear();
    }

    private WebSocketSession session(String id, String query) throws Exception {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.getUri()).thenReturn(URI.create("ws://localhost/ws/chat" + (query != null ? "?" + query : "")));
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void afterConnectionEstablished_validToken_registers() throws Exception {
        when(jwtTokenProvider.validateAccessToken("valid")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid")).thenReturn(1L);
        WebSocketSession s = session("s1", "token=valid");

        server.afterConnectionEstablished(s);

        assertEquals(1L, WebSocketServer.ONLINE_MAP.get(s));
        assertTrue(WebSocketServer.USER_CHANNELS.containsKey(1L));
        assertTrue(WebSocketServer.USER_CHANNELS.get(1L).contains(s));
        verify(s, never()).close();
    }

    @Test
    void afterConnectionEstablished_invalidToken_closes() throws Exception {
        when(jwtTokenProvider.validateAccessToken("bad")).thenReturn(false);
        WebSocketSession s = session("s1", "token=bad");

        server.afterConnectionEstablished(s);

        assertNull(WebSocketServer.ONLINE_MAP.get(s));
        verify(s).close();
    }

    @Test
    void handleTextMessage_chat_isRejected_notForwarded() throws Exception {
        WebSocketSession sender = session("sender", null);
        WebSocketSession recipient = session("recipient", null);
        WebSocketServer.ONLINE_MAP.put(sender, 1L);
        WebSocketServer.USER_CHANNELS.computeIfAbsent(2L, k -> new CopyOnWriteArraySet<>()).add(recipient);

        server.handleTextMessage(sender,
                new TextMessage("{\"type\":\"chat\",\"toUserId\":2,\"content\":\"hi\",\"fromUserId\":999}"));

        // 接收者不得收到任何直接转发的内容
        verify(recipient, never()).sendMessage(any());
        // 发送端收到 chat_rejected 回执
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(sender).sendMessage(captor.capture());
        WsMessage reply = objectMapper.readValue(captor.getValue().getPayload(), WsMessage.class);
        assertEquals("chat_rejected", reply.getType());
    }

    @Test
    void handleTextMessage_heartbeat_returnsPong() throws Exception {
        WebSocketSession s = session("s1", null);
        WebSocketServer.ONLINE_MAP.put(s, 1L);

        server.handleTextMessage(s, new TextMessage("{\"type\":\"heartbeat\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(s).sendMessage(captor.capture());
        WsMessage reply = objectMapper.readValue(captor.getValue().getPayload(), WsMessage.class);
        assertEquals("pong", reply.getType());
    }
}
