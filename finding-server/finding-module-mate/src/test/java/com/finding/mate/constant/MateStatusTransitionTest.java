package com.finding.mate.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-2 状态迁移表:搭子活动/报名状态的非法迁移判定。
 * 活动:仅 ACTIVE 可流转;报名:按 PENDING/WAITLISTED/ACCEPTED 规则,INVALIDATED 为强制失效。
 */
class MateStatusTransitionTest {

    // ── MateInvitationStatus ──
    @Test
    void invitation_activeCanCloseCancelOrExpire() {
        assertTrue(MateInvitationStatus.ACTIVE.canTransitTo(MateInvitationStatus.CLOSED));
        assertTrue(MateInvitationStatus.ACTIVE.canTransitTo(MateInvitationStatus.CANCELLED));
        assertTrue(MateInvitationStatus.ACTIVE.canTransitTo(MateInvitationStatus.EXPIRED));
    }

    @Test
    void invitation_terminalStatesAreImmutable() {
        for (MateInvitationStatus terminal : new MateInvitationStatus[]{
                MateInvitationStatus.CANCELLED, MateInvitationStatus.CLOSED, MateInvitationStatus.EXPIRED}) {
            for (MateInvitationStatus next : MateInvitationStatus.values()) {
                assertFalse(terminal.canTransitTo(next), terminal + " 不应流转到 " + next);
            }
        }
    }

    // ── MateParticipantStatus ──
    @Test
    void participant_pendingCanFlow() {
        assertTrue(MateParticipantStatus.PENDING.canTransitTo(MateParticipantStatus.ACCEPTED));
        assertTrue(MateParticipantStatus.PENDING.canTransitTo(MateParticipantStatus.REJECTED));
        assertTrue(MateParticipantStatus.PENDING.canTransitTo(MateParticipantStatus.CANCELLED));
        assertTrue(MateParticipantStatus.PENDING.canTransitTo(MateParticipantStatus.WAITLISTED));
    }

    @Test
    void participant_waitlistedCanAcceptOrCancel() {
        assertTrue(MateParticipantStatus.WAITLISTED.canTransitTo(MateParticipantStatus.ACCEPTED));
        assertTrue(MateParticipantStatus.WAITLISTED.canTransitTo(MateParticipantStatus.CANCELLED));
    }

    @Test
    void participant_acceptedCanLeave() {
        assertTrue(MateParticipantStatus.ACCEPTED.canTransitTo(MateParticipantStatus.CANCELLED));
    }

    @Test
    void participant_illegalTransitionsRejected() {
        // 已拒绝不可再通过/候补
        assertFalse(MateParticipantStatus.REJECTED.canTransitTo(MateParticipantStatus.ACCEPTED));
        assertFalse(MateParticipantStatus.REJECTED.canTransitTo(MateParticipantStatus.WAITLISTED));
        // PENDING 不可直接候补后又退回,也不可原地不动
        assertFalse(MateParticipantStatus.PENDING.canTransitTo(MateParticipantStatus.PENDING));
        // 候补不可直接拒绝
        assertFalse(MateParticipantStatus.WAITLISTED.canTransitTo(MateParticipantStatus.REJECTED));
        // 已退出不可复入
        assertFalse(MateParticipantStatus.CANCELLED.canTransitTo(MateParticipantStatus.ACCEPTED));
    }

    @Test
    void participant_invalidatedIsForcedFromAnyNonTerminal() {
        for (MateParticipantStatus s : MateParticipantStatus.values()) {
            if (s != MateParticipantStatus.INVALIDATED) {
                assertTrue(s.canTransitTo(MateParticipantStatus.INVALIDATED), s + " 可被强制失效");
            }
        }
        // 已失效不可再失效
        assertFalse(MateParticipantStatus.INVALIDATED.canTransitTo(MateParticipantStatus.INVALIDATED));
    }
}
