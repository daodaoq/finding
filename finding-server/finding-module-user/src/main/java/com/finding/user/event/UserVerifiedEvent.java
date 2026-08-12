package com.finding.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学生实名认证审核完成事件 —— user 模块发布,由 app 模块监听发站内通知。
 * 通过:通知用户"认证已通过";驳回:通知用户原因。
 */
@Getter
@AllArgsConstructor
public class UserVerifiedEvent {

    /** 被审核用户 */
    private final Long userId;
    /** true=审核通过, false=驳回 */
    private final boolean approved;
    /** 驳回原因(通过时为空) */
    private final String comment;
    /** 操作管理员 */
    private final Long operatorId;
}
