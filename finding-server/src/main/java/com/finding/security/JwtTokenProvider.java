package com.finding.security;

import io.jsonwebtoken.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT 令牌提供器 —— 生成、验证、解析 JWT，并从中构建 Spring Security Authentication。
 * 替代旧的 JwtUtils，与 Spring Security 体系完全集成。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${jwt.access-secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        byte[] accessBytes = accessSecret.getBytes(StandardCharsets.UTF_8);
        byte[] refreshBytes = refreshSecret.getBytes(StandardCharsets.UTF_8);
        // 确保密钥长度 ≥ 256 bits (HS256 最低要求)
        if (accessBytes.length * 8 < 256) throw new IllegalArgumentException("JWT access 密钥过短，需 ≥ 256 bits");
        if (refreshBytes.length * 8 < 256) throw new IllegalArgumentException("JWT refresh 密钥过短，需 ≥ 256 bits");
        this.accessKey = new SecretKeySpec(accessBytes, "HmacSHA256");
        this.refreshKey = new SecretKeySpec(refreshBytes, "HmacSHA256");
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    // ── 生成令牌 ──

    /** 从 Authentication 创建访问令牌，载荷包含 userId 和权限列表 */
    public String createAccessToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String userId = principal instanceof Long ? principal.toString()
                : principal instanceof UserPrincipal ? String.valueOf(((UserPrincipal) principal).getId())
                : principal.toString();

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("auth", authorities)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(accessKey)
                .compact();
    }

    /** 创建刷新令牌，载荷仅包含 userId */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(refreshKey)
                .compact();
    }

    // ── 解析令牌 → Authentication ──

    /** 从 JWT 中解析出 Spring Security Authentication 对象 */
    public Authentication getAuthentication(String token) {
        Claims claims = parseAccessToken(token);
        Long userId = Long.valueOf(claims.getSubject());

        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get("auth", String.class).split(","))
                .filter(s -> !s.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserPrincipal principal = new UserPrincipal(userId, claims.getSubject());
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /** 从令牌中提取用户 ID */
    public Long getUserIdFromToken(String token) {
        try {
            return Long.valueOf(parseAccessToken(token).getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    // ── 验证令牌 ──

    /** 验证访问令牌是否合法（签名正确且未过期） */
    public boolean validateAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 令牌无效: {}", e.getMessage());
            return false;
        }
    }

    /** 验证刷新令牌是否合法 */
    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("刷新令牌无效: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessExpiration() {
        return accessExpiration;
    }

    // ── 内部方法 ──

    private Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(accessKey).build()
                .parseSignedClaims(token).getPayload();
    }

    private Claims parseRefreshToken(String token) {
        return Jwts.parser().verifyWith(refreshKey).build()
                .parseSignedClaims(token).getPayload();
    }
}
