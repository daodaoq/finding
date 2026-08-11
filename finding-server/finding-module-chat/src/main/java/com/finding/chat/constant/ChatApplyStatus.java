package com.finding.chat.constant;

import lombok.Getter;

/**
 * 聊天申请状态机: PENDING -> APPROVED | REJECTED | CANCELLED | EXPIRED
 * <ul>
 *   <li>PENDING  : 申请人可撤回,接收人可同意或拒绝</li>
 *   <li>APPROVED : 创建唯一私聊会话</li>
 *   <li>REJECTED : 冷却期后可再次申请</li>
 *   <li>CANCELLED: 申请人主动撤回 或 拉黑后自动取消,接收人不可再处理</li>
 *   <li>EXPIRED  : 超过期限未处理,惰性过期</li>
 * </ul>
 */
@Getter
public enum ChatApplyStatus {

    PENDING(0, "待通过"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已撤回"),
    EXPIRED(4, "已过期");

    private final int code;
    private final String desc;

    ChatApplyStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        if (code == null) return "未知";
        for (ChatApplyStatus s : values()) {
            if (s.code == code) return s.desc;
        }
        return "未知";
    }

    /** 状态迁移是否合法:仅 PENDING 可流转(通过/拒绝/撤回/过期),其余均为终态 */
    public boolean canTransitTo(ChatApplyStatus next) {
        return next != null && this == PENDING;
    }
}
