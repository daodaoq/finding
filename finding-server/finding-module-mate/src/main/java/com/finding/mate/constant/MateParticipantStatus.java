package com.finding.mate.constant;

import lombok.Getter;

/**
 * 搭子报名状态: PENDING -> ACCEPTED | REJECTED | CANCELLED | WAITLISTED
 * <ul>
 *   <li>PENDING   : 待发起人审批</li>
 *   <li>ACCEPTED  : 已通过(占用名额)</li>
 *   <li>REJECTED  : 被发起人拒绝</li>
 *   <li>CANCELLED : 已退出(成员取消,保留审计)</li>
 *   <li>WAITLISTED: 名额已满进入候补,有空位时按报名时间补位</li>
 * </ul>
 */
@Getter
public enum MateParticipantStatus {

    PENDING(0, "待审核"),
    ACCEPTED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已退出"),
    WAITLISTED(4, "候补中"),
    INVALIDATED(5, "活动已结束");


    private final int code;
    private final String desc;

    MateParticipantStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        if (code == null) return "未知";
        for (MateParticipantStatus s : values()) {
            if (s.code == code) return s.desc;
        }
        return "未知";
    }

    /**
     * 状态迁移是否合法(状态迁移表):
     * PENDING -> ACCEPTED | REJECTED | CANCELLED | WAITLISTED
     * WAITLISTED -> ACCEPTED | CANCELLED
     * ACCEPTED -> CANCELLED
     * INVALIDATED(活动结束强制失效)允许从任意非失效态转入
     */
    public boolean canTransitTo(MateParticipantStatus next) {
        if (next == null) return false;
        if (next == INVALIDATED) return this != INVALIDATED;
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == REJECTED || next == CANCELLED || next == WAITLISTED;
            case WAITLISTED -> next == ACCEPTED || next == CANCELLED;
            case ACCEPTED -> next == CANCELLED;
            default -> false;
        };
    }
}
