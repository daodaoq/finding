package com.finding.framework.util;

import com.finding.common.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式固定窗口限流器(基于 Redis,多实例共享)。
 * 以 key(如 userId + 动作)维度,在 windowMs 窗口内最多放行 limit 次。
 * 用于举报、私信发送等反骚扰限流;替代原进程内内存实现,避免多实例/重启后计数失效。
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final RedisUtils redisUtils;

    /** 是否放行本次请求(达到窗口上限返回 false) */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        // INCR 原子自增;首次计数(==1)时设置窗口过期,窗口结束后计数自动归零
        long count = redisUtils.increment(key, 1);
        if (count == 1) {
            redisUtils.expire(key, windowMs, TimeUnit.MILLISECONDS);
        }
        return count <= limit;
    }
}
