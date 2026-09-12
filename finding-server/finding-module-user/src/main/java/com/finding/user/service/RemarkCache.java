package com.finding.user.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 备注查询的内存微缓存 —— 备注是"查看者 × 目标"的私密别名。
 *
 * 存在意义:资料/会话/评论等列表接口会逐行组装昵称,直接查库会变成每行 +1 次 SQL。
 *
 * 一致性:单进程部署(nohup java -jar),写路径(setRemark/clearRemark)主动 evict,
 * 因此不存在"改完备注还看到旧值"的情况;TTL 只是兜底上限。
 * 若将来改为多实例部署,应删除本缓存(退化为 TTL 内的短暂陈旧),或换成集中式缓存。
 */
@Component
public class RemarkCache {

    private static final long TTL_MS = 60_000L;
    /** 硬上限:单进程 -Xmx256m,条目为两个 Long + 短字符串,超限直接清空以严格有界 */
    private static final int MAX_ENTRIES = 10_000;

    /** remark 为空串表示"已确认无备注",以此区分"未缓存(null)" */
    private record Entry(String remark, long expireAt) {}

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    /** null = 未缓存;"" = 已缓存"无备注";其余为备注值 */
    public String get(Long viewerId, Long targetId) {
        String key = key(viewerId, targetId);
        Entry entry = cache.get(key);
        if (entry == null) return null;
        if (entry.expireAt() < System.currentTimeMillis()) {
            cache.remove(key);
            return null;
        }
        return entry.remark();
    }

    public void put(Long viewerId, Long targetId, String remark) {
        if (cache.size() >= MAX_ENTRIES) cache.clear();
        cache.put(key(viewerId, targetId),
                new Entry(remark == null ? "" : remark, System.currentTimeMillis() + TTL_MS));
    }

    public void evict(Long viewerId, Long targetId) {
        if (viewerId == null || targetId == null) return;
        cache.remove(key(viewerId, targetId));
    }

    private String key(Long viewerId, Long targetId) {
        return viewerId + ":" + targetId;
    }
}
