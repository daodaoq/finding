package com.finding.mate.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.mate.constant.MateParticipantStatus;
import com.finding.mate.entity.MateInvitation;
import com.finding.mate.entity.MateParticipant;
import com.finding.mate.mapper.MateInvitationMapper;
import com.finding.mate.mapper.MateParticipantMapper;
import com.finding.message.service.MessageService;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserService;
import com.finding.user.service.UserWriteGuard;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 搭子预约状态机单测 —— 满员进候补 / 并发名额防超卖 / 拉黑 / 退出补位。
 */
@ExtendWith(MockitoExtension.class)
class MateServiceImplTest {

    @Mock private MateInvitationMapper invitationMapper;
    @Mock private MateParticipantMapper participantMapper;
    @Mock private MessageService messageService;
    @Mock private UserMapper userMapper;
    @Mock private UserService userService;
    @Mock private SensitiveWordFilter sensitiveWordFilter;
    @Mock private UserRelationshipService relationshipService;
    @Mock private UserWriteGuard userWriteGuard;

    @InjectMocks
    private MateServiceImpl service;

    @BeforeEach
    void initMybatisLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), MateInvitation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), MateParticipant.class);
    }

    // ── joinInvitation ──

    @Test
    void join_notFull_createsPending() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(relationshipService.isBlockedEitherWay(2L, 100L)).thenReturn(false);
        when(participantMapper.selectCount(any())).thenReturn(0L);
        when(participantMapper.insert(any())).thenReturn(1);

        service.joinInvitation(2L, 1L, "hi");

        ArgumentCaptor<MateParticipant> cap = ArgumentCaptor.forClass(MateParticipant.class);
        verify(participantMapper).insert(cap.capture());
        assertEquals(MateParticipantStatus.PENDING.getCode(), cap.getValue().getStatus());
    }

    @Test
    void join_full_goesWaitlist() {
        MateInvitation inv = invitation(1L, 10, 10);
        inv.setUserId(100L);
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(relationshipService.isBlockedEitherWay(2L, 100L)).thenReturn(false);
        when(participantMapper.selectCount(any())).thenReturn(0L);
        when(participantMapper.insert(any())).thenReturn(1);

        service.joinInvitation(2L, 1L, "hi");

        ArgumentCaptor<MateParticipant> cap = ArgumentCaptor.forClass(MateParticipant.class);
        verify(participantMapper).insert(cap.capture());
        assertEquals(MateParticipantStatus.WAITLISTED.getCode(), cap.getValue().getStatus());
    }

    @Test
    void join_blocked_rejected() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(relationshipService.isBlockedEitherWay(2L, 100L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.joinInvitation(2L, 1L, "hi"));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    // ── handleJoinRequest ──

    @Test
    void handleAccept_whenFull_capacityBlocked() {
        MateInvitation inv = invitation(1L, 10, 10);
        inv.setUserId(100L);
        MateParticipant part = participant(2L, 1L, 200L, MateParticipantStatus.PENDING.getCode());
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(participantMapper.selectById(2L)).thenReturn(part);
        when(relationshipService.isBlockedEitherWay(100L, 200L)).thenReturn(false);
        when(participantMapper.update(any(), any())).thenReturn(1); // 报名置为已通过
        when(invitationMapper.update(any(), any())).thenReturn(0);  // 原子名额自增失败 → 已满

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handleJoinRequest(100L, 1L, 2L, true));
        assertEquals(ResultCode.MATE_FULL.getCode(), ex.getCode());
    }

    @Test
    void handleAccept_ok_notifies() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        MateParticipant part = participant(2L, 1L, 200L, MateParticipantStatus.PENDING.getCode());
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(participantMapper.selectById(2L)).thenReturn(part);
        when(relationshipService.isBlockedEitherWay(100L, 200L)).thenReturn(false);
        when(participantMapper.update(any(), any())).thenReturn(1);
        when(invitationMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.handleJoinRequest(100L, 1L, 2L, true));
        verify(messageService).notify(any(), any(), eq("mate_accepted"), any(), any());
    }

    @Test
    void handle_blocked_rejected() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        MateParticipant part = participant(2L, 1L, 200L, MateParticipantStatus.PENDING.getCode());
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(participantMapper.selectById(2L)).thenReturn(part);
        when(relationshipService.isBlockedEitherWay(100L, 200L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handleJoinRequest(100L, 1L, 2L, true));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    // ── leaveInvitation: 退出释放名额 + 候补补位 ──

    @Test
    void leave_accepted_promotesWaitlist() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        MateParticipant leaver = participant(5L, 1L, 200L, MateParticipantStatus.ACCEPTED.getCode());
        MateParticipant waitlisted = participant(6L, 1L, 300L, MateParticipantStatus.WAITLISTED.getCode());
        when(participantMapper.selectOne(any())).thenReturn(leaver, waitlisted); // 先查退出者,再查候补
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(invitationMapper.update(any(), any())).thenReturn(1); // 释放名额 + 补位占用名额
        when(participantMapper.update(any(), any())).thenReturn(1); // 候补提升
        when(participantMapper.updateById(any())).thenReturn(1);    // 退出者置为已退出

        assertDoesNotThrow(() -> service.leaveInvitation(200L, 1L));
        verify(messageService).notify(any(), any(), eq("mate_accepted"), any(), any()); // 补位通知
    }

    private MateInvitation invitation(Long id, int current, int max) {
        MateInvitation inv = new MateInvitation();
        inv.setId(id);
        inv.setCurrentParticipants(current);
        inv.setMaxParticipants(max);
        inv.setStatus(1);
        inv.setActivityTime(LocalDateTime.now().plusDays(1));
        return inv;
    }

    private MateParticipant participant(Long id, Long invitationId, Long userId, int status) {
        MateParticipant p = new MateParticipant();
        p.setId(id);
        p.setInvitationId(invitationId);
        p.setUserId(userId);
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }
}
