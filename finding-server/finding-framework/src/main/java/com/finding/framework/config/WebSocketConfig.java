package com.finding.framework.config;

import com.finding.framework.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;

/**
 * WebSocket 配置 —— 注册自定义 WebSocketServer 处理器。
 * 允许来源由 finding.websocket.allowed-origin-patterns 配置(逗号分隔的 origin 模式);
 * 未配置时回退 * 并告警,以免破坏部署。
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketServer webSocketServer;

    @Value("${finding.websocket.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        List<String> patterns = StringUtils.hasText(allowedOriginPatterns)
                ? Arrays.stream(allowedOriginPatterns.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList()
                : List.of();

        if (patterns.isEmpty()) {
            log.warn("finding.websocket.allowed-origin-patterns 未配置,WebSocket 允许所有来源(生产环境请配置可信前端域名)");
            registry.addHandler(webSocketServer, "/ws/chat")
                    .setAllowedOriginPatterns("*");
        } else {
            registry.addHandler(webSocketServer, "/ws/chat")
                    .setAllowedOriginPatterns(patterns.toArray(new String[0]));
        }
    }
}
