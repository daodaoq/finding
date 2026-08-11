package com.finding.framework.config;

import com.finding.user.security.JwtInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 请求日志 —— 输出 requestId、操作者、方法、路径、状态与耗时,便于线上问题复盘。
 */
@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("_start", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute("_start");
        long cost = start != null ? System.currentTimeMillis() - (Long) start : 0;
        Long userId = null;
        try {
            userId = JwtInterceptor.getCurrentUserId();
        } catch (Exception ignored) {
            // 未登录或上下文异常时忽略
        }
        log.info("[req] id={} user={} method={} path={} status={} cost={}ms",
                UUID.randomUUID().toString().substring(0, 8),
                userId, request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
    }
}
