package com.finding.user.config;

import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security JWT 认证过滤器。
 * 每次请求时从 Authorization 头提取 JWT，
 * 校验通过后将 Authentication 注入 SecurityContext。
 * 这是整个系统中唯一的鉴权入口 —— 不再需要 Interceptor 做鉴权。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateAccessToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            // 校验账号仍有效:封禁/冻结/注销即时生效,已登录的旧 token 也会被拒
            if (userId != null && isActive(userId)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT 认证成功，已设置 SecurityContext");
            }
        }

        filterChain.doFilter(request, response);
    }

    /** 账号是否处于正常状态(封禁/冻结的 token 不再放行) */
    private boolean isActive(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getStatus() != null && user.getStatus() == 1;
    }

    /** 从 Authorization 头提取 Bearer Token */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
