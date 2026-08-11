package com.finding.framework.config;

import com.finding.user.security.JwtInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 请求日志 —— 生成并贯穿 traceId(写入 MDC 与 X-Trace-Id 响应头),
 * 输出操作者、方法、路径、状态与耗时。线上问题可凭响应头 traceId 关联后端日志。
 */
@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("_start", System.currentTimeMillis());
        // traceId:贯穿该请求的全部日志(Result 也会携带),并通过响应头暴露给调用方
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute("_start");
        long cost = start != null ? System.currentTimeMillis() - (Long) start : 0;
        Long userId = null;
        try {
            userId = JwtInterceptor.getCurrentUserId();
        } catch (Exception e) {
            // 未登录或上下文异常属预期可忽略:降级 debug 记录,便于偶发上下文损坏时排查
            log.debug("获取当前用户失败(未登录或上下文异常), 请求: {} {}", request.getMethod(), request.getRequestURI(), e);
        }
        log.info("[req] id={} user={} method={} path={} status={} cost={}ms",
                MDC.get("traceId"),
                userId, request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
        // 请求结束清理,避免 traceId 泄漏到线程池复用线程的下一个请求
        MDC.remove("traceId");
    }
}
