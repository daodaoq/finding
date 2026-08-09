package com.finding.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户被封禁事件 —— user 模块发布,由 app 模块监听后经 WebSocket 实时推送给被封禁用户。
 * 避免 user(叶子模块)反向依赖 framework。
 */
@Getter
@AllArgsConstructor
public class UserBannedEvent {

    private final Long userId;
    private final String reason;
    /** 封禁到期时间;null 表示永久封禁 */
    private final LocalDateTime bannedUntil;
}
