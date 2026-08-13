package com.finding.bridge.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-2 状态迁移表:校验枚举上的非法迁移判定。
 * 聊天申请:仅 PENDING 可流转;信息互换:PENDING->APPROVED|REJECTED,REJECTED->PENDING。
 */
class StateTransitionTest {

    // ── ChatApplyStatus ──
    @Test
    void chatApply_pendingCanFlowToAny() {
        assertTrue(ChatApplyStatus.PENDING.canTransitTo(ChatApplyStatus.APPROVED));
        assertTrue(ChatApplyStatus.PENDING.canTransitTo(ChatApplyStatus.REJECTED));
        assertTrue(ChatApplyStatus.PENDING.canTransitTo(ChatApplyStatus.CANCELLED));
        assertTrue(ChatApplyStatus.PENDING.canTransitTo(ChatApplyStatus.EXPIRED));
    }

    @Test
    void chatApply_terminalStatesAreImmutable() {
        for (ChatApplyStatus terminal : new ChatApplyStatus[]{
                ChatApplyStatus.APPROVED, ChatApplyStatus.REJECTED,
                ChatApplyStatus.CANCELLED, ChatApplyStatus.EXPIRED}) {
            for (ChatApplyStatus next : ChatApplyStatus.values()) {
                assertFalse(terminal.canTransitTo(next), terminal + " 不应流转到 " + next);
            }
        }
    }

    @Test
    void chatApply_nullNextRejected() {
        assertFalse(ChatApplyStatus.PENDING.canTransitTo(null));
    }

    // ── InfoShareStatus ──
    @Test
    void infoShare_pendingCanApproveOrReject() {
        assertTrue(InfoShareStatus.PENDING.canTransitTo(InfoShareStatus.APPROVED));
        assertTrue(InfoShareStatus.PENDING.canTransitTo(InfoShareStatus.REJECTED));
    }

    @Test
    void infoShare_rejectedCanReapply() {
        // 被拒绝后可再次申请:REJECTED -> PENDING
        assertTrue(InfoShareStatus.REJECTED.canTransitTo(InfoShareStatus.PENDING));
    }

    @Test
    void infoShare_illegalTransitionsRejected() {
        // 已同意是终态,不可再流转
        assertFalse(InfoShareStatus.APPROVED.canTransitTo(InfoShareStatus.REJECTED));
        assertFalse(InfoShareStatus.APPROVED.canTransitTo(InfoShareStatus.PENDING));
        // PENDING 不可原地不动,更不可回退
        assertFalse(InfoShareStatus.PENDING.canTransitTo(InfoShareStatus.PENDING));
        assertFalse(InfoShareStatus.REJECTED.canTransitTo(InfoShareStatus.APPROVED));
        assertFalse(InfoShareStatus.REJECTED.canTransitTo(InfoShareStatus.REJECTED));
    }

    @Test
    void infoShare_ofResolvesCode() {
        assertTrue(InfoShareStatus.of(0) == InfoShareStatus.PENDING);
        assertTrue(InfoShareStatus.of(1) == InfoShareStatus.APPROVED);
        assertTrue(InfoShareStatus.of(2) == InfoShareStatus.REJECTED);
        assertTrue(InfoShareStatus.of(null) == null);
        assertTrue(InfoShareStatus.of(99) == null);
    }
}
