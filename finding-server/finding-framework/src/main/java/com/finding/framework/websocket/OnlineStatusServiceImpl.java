package com.finding.framework.websocket;

import com.finding.common.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private static final String KEY_PREFIX = "presence:online:";
    /** TTL 略大于客户端心跳周期(30s),心跳中断即自动判定离线 */
    private static final long TTL_SECONDS = 75;

    private final RedisUtils redisUtils;

    @Override
    public void markOnline(Long userId) {
        if (userId == null) return;
        redisUtils.set(KEY_PREFIX + userId, "1", TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void markOffline(Long userId) {
        if (userId == null) return;
        redisUtils.delete(KEY_PREFIX + userId);
    }

    @Override
    public boolean isOnline(Long userId) {
        return userId != null && redisUtils.exists(KEY_PREFIX + userId);
    }

    @Override
    public Map<Long, Boolean> isOnlineBatch(Collection<Long> userIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return result;
        for (Long id : userIds) {
            if (id != null) result.put(id, isOnline(id));
        }
        return result;
    }
}
