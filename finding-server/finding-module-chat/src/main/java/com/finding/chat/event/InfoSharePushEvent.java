package com.finding.chat.event;

/**
 * 信息互换 WS 推送事件 —— 事务内发布,由 {@link InfoSharePushListener} 在事务提交后投递,
 * 避免事务回滚时产生"已推送但库中无记录"的不一致。
 */
public record InfoSharePushEvent(Long toUserId, String action, Long fromUserId, String content, Long shareId) {
}
