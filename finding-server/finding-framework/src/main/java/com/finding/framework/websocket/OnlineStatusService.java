package com.finding.framework.websocket;

import java.util.Collection;
import java.util.Map;

/**
 * 在线状态服务 —— Redis 心跳键维护「实时在线」。
 * WebSocket 连接建立/心跳时刷新 TTL,断开时删除;REST 可据此查询在线状态(跨实例、重启不残留)。
 */
public interface OnlineStatusService {

    /** 标记在线(写入心跳键并刷新 TTL) */
    void markOnline(Long userId);

    /** 标记离线(删除心跳键) */
    void markOffline(Long userId);

    /** 是否在线 */
    boolean isOnline(Long userId);

    /** 批量查询在线状态 */
    Map<Long, Boolean> isOnlineBatch(Collection<Long> userIds);
}
