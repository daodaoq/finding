package com.finding.message.event;

/**
 * 新站内通知事件 —— 由 {@code MessageServiceImpl.notify} 发布,
 * finding-app 中的监听器收到后通过 WebSocket 实时推送,用户端刷新未读角标。
 */
public record NewNotificationEvent(Long toUserId) {
}
