package com.finding.mate.constant;

import lombok.Getter;

/**
 * 搭子活动状态: ACTIVE(OPEN) -> CLOSED | CANCELLED。
 * FULL(满员)为派生状态；EXPIRED 由定时任务收敛并落库，便于历史审计。
 */
@Getter
public enum MateInvitationStatus {

    CANCELLED(0, "已取消"),
    ACTIVE(1, "进行中"),
    CLOSED(2, "已关闭"),
    EXPIRED(3, "已过期");

    private final int code;
    private final String desc;

    MateInvitationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 状态迁移是否合法:仅 ACTIVE 可流转(关闭/取消/过期),其余均为终态 */
    public boolean canTransitTo(MateInvitationStatus next) {
        return next != null && this == ACTIVE;
    }
}
