package com.finding.bridge.constant;

import lombok.Getter;

/**
 * 信息互换申请状态机: PENDING -> APPROVED | REJECTED;REJECTED -> PENDING(可再次申请)。
 * <ul>
 *   <li>PENDING  : 接收方可同意或拒绝</li>
 *   <li>APPROVED : 互换生效,双方可见对方详情(终态)</li>
 *   <li>REJECTED : 拒绝后可再次申请(原地改回 PENDING)</li>
 * </ul>
 */
@Getter
public enum InfoShareStatus {

    PENDING(0, "待处理"),
    APPROVED(1, "已同意"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String desc;

    InfoShareStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 状态迁移是否合法(状态迁移表) */
    public boolean canTransitTo(InfoShareStatus next) {
        if (next == null) return false;
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED;
            case REJECTED -> next == PENDING;
            case APPROVED -> false;
        };
    }

    /** 由 code 解析枚举;未知值返回 null */
    public static InfoShareStatus of(Integer code) {
        if (code == null) return null;
        for (InfoShareStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
