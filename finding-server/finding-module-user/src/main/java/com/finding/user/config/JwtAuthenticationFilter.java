package com.finding.user.config;

import com.finding.common.RedisUtils;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtTokenProvider;
import com.finding.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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
    private final RedisUtils redisUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && !isBlacklisted(token) && jwtTokenProvider.validateAccessToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            // 校验账号仍有效(封禁/冻结/注销即时失效);角色从数据库重载,不信任令牌内的 auth claim
            User user = userId != null ? loadActiveUser(userId) : null;
            if (user != null) {
                String role = user.getRole() != null ? user.getRole() : "user";
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                UserPrincipal principal = new UserPrincipal(userId, String.valueOf(userId));
                Authentication auth = new UsernamePasswordAuthenticationToken(principal, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT 认证成功，已设置 SecurityContext");
            }
        }

        filterChain.doFilter(request, response);
    }

    /** 是否已被登出拉黑(Redis 中 token:blacklist:{token} 存在);Redis 异常时降级放行,由令牌过期时间兜底 */
    private boolean isBlacklisted(String token) {
        try {
            return redisUtils.exists(JwtTokenProvider.TOKEN_BLACKLIST_PREFIX + token);
        } catch (Exception e) {
            log.debug("读取令牌黑名单失败,降级放行: {}", e.getMessage());
            return false;
        }
    }

    /** 加载状态正常的账号(封禁/冻结/注销返回 null) */
    private User loadActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getStatus() != null && user.getStatus() == 1) {
            return user;
        }
        return null;
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
