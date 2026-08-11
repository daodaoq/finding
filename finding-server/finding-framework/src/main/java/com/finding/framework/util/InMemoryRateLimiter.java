package com.finding.framework.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量内存固定窗口限流器(单机适用)。
 * 以 key(如 userId + 动作)维度,在 windowMs 窗口内最多放行 limit 次。
 * 用于举报、私信发送等反骚扰限流;多实例部署时需替换为 Redis 实现。
 */
@Component
public class InMemoryRateLimiter {

    private static final class Bucket {
        final AtomicInteger count = new AtomicInteger();
        volatile long windowStart = System.currentTimeMillis();
        volatile long lastAccess = System.currentTimeMillis();
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** 是否放行本次请求(达到窗口上限返回 false) */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        synchronized (bucket) {
            if (now - bucket.windowStart >= windowMs) {
                bucket.windowStart = now;
                bucket.count.set(0);
            }
            boolean allowed = bucket.count.get() < limit;
            if (allowed) bucket.count.incrementAndGet();
            bucket.lastAccess = now;
            return allowed;
        }
    }

    /** 清理长期未访问的桶,防止内存无限增长(每次调用抽样执行) */
    public void cleanup(long idleMs) {
        long now = System.currentTimeMillis();
        if (now % 10 != 0) return; // 抽样,避免每次请求全表扫描
        buckets.entrySet().removeIf(e -> now - e.getValue().lastAccess > idleMs);
    }
}
