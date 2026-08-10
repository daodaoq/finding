package com.finding.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户拉黑他人事件 —— user 模块发布,由各业务模块(如 chat)监听做联动:
 * 例如自动取消双方之间的待处理聊天申请。
 * 事件类放在 common 以便发布方(user 叶子模块)与监听方(chat)都能访问,避免环依赖。
 */
@Getter
@AllArgsConstructor
public class UserBlockedEvent {

    /** 拉黑发起方 */
    private final Long userId;
    /** 被拉黑用户 */
    private final Long blockedUserId;
}
