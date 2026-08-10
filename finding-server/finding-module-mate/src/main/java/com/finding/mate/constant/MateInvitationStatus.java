package com.finding.mate.constant;

import lombok.Getter;

/**
 * 搭子活动状态: ACTIVE(OPEN) -> CLOSED | CANCELLED。
 * FULL(满员)与 EXPIRED(已过期)为派生状态,由 current_participants/max 与 activity_time 计算,不落库。
 */
@Getter
public enum MateInvitationStatus {

    CANCELLED(0, "已取消"),
    ACTIVE(1, "进行中"),
    CLOSED(2, "已关闭");

    private final int code;
    private final String desc;

    MateInvitationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
