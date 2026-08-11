package com.finding.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 用户收到警告事件 —— user 模块发布,由 app 模块监听发站内通知。 */
@Getter
@AllArgsConstructor
public class UserWarningEvent {

    private final Long userId;
    private final String reason;
    private final Long operatorId;
}
