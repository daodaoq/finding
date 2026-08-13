package com.finding.framework.websocket;

import com.finding.common.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 在线状态服务 —— Redis 心跳键维护「实时在线」。
 * WebSocket 连接建立/心跳时刷新 TTL,断开时删除;REST 可据此查询在线状态(跨实例、重启不残留)。
 */
@Component
@RequiredArgsConstructor
public class OnlineStatusService {

    private static final String KEY_PREFIX = "presence:online:";
    /** TTL 略大于客户端心跳周期(30s),心跳中断即自动判定离线 */
    private static final long TTL_SECONDS = 75;

    public void markOnline(Long userId) {
        if (userId == null) return;
        redisUtils.set(KEY_PREFIX + userId, "1", TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void markOffline(Long userId) {
        if (userId == null) return;
        redisUtils.delete(KEY_PREFIX + userId);
    }

    public boolean isOnline(Long userId) {
        return userId != null && redisUtils.exists(KEY_PREFIX + userId);
    }

    /** 批量查询在线状态 */
    public Map<Long, Boolean> isOnlineBatch(Collection<Long> userIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return result;
        for (Long id : userIds) {
            if (id != null) result.put(id, isOnline(id));
        }
        return result;
    }

    private final RedisUtils redisUtils;
}
