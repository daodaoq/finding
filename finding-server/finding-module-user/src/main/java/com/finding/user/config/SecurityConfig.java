package com.finding.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    // ── Beans ──

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    // ── Security filter chain ──

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable things we don't need for REST API
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // 未认证统一返回 401(此前无 entryPoint,走了 Spring Security 默认的 403)。
            // 语义修正:401=未认证(前端据此静默刷新 token),403=已认证但无权限。
            // 前端 request.ts 只在 401 时尝试刷新,403 直接登出 —— 这正是用户被频繁强制
            // 登出的原因之一。管理端 request.ts 对 401/403 处理相同,不受影响。
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            // Register our JWT filter BEFORE UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Set the provider
            .authenticationProvider(authenticationProvider())

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // ── Public ──
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/send-code",
                    "/api/v1/auth/captcha",
                    "/api/v1/auth/refresh"
                ).permitAll()

                .requestMatchers(HttpMethod.GET,
                    "/api/v1/posts",
                    "/api/v1/posts/{id}",
                    "/api/v1/posts/{id}/comments",
                    "/api/v1/mates",
                    "/api/v1/mates/{id}",
                    "/api/v1/mates/categories",
                    "/api/v1/home/banners",
                    "/api/v1/home/announcements",
                    "/api/v1/search",
                    "/api/v1/users/{id}",
                    "/api/v1/users/search",
                    "/api/v1/love-guides"          // 恋爱干货公开列表(GET);投稿 POST 仍要求登录
                ).permitAll()

                // Swagger / Knife4j
                .requestMatchers(
                    "/doc.html", "/webjars/**", "/v3/api-docs/**",
                    "/swagger-resources/**", "/swagger-ui/**"
                ).permitAll()

                // Static uploads & image proxy
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/v1/images/**").permitAll()

                // WebSocket
                .requestMatchers("/ws/**", "/ws/info").permitAll()

                // ── Admin only ──
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // ── Everything else: authenticated ──
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
