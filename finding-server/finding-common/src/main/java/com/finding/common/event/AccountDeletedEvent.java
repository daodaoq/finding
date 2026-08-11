package com.finding.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户注销账号事件 —— user 模块发布,由各业务模块(如 chat)监听做联动:
 * 例如取消涉及该用户的待处理聊天申请、信息互换申请。
 * 事件类放在 common 以便发布方(user 叶子模块)与监听方(chat)都能访问,避免环依赖。
 */
@Getter
@AllArgsConstructor
public class AccountDeletedEvent {

    /** 被注销的用户 ID */
    private final Long userId;
}
