package com.finding.mate.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.word.ReviewResult;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.mate.constant.MateParticipantStatus;
import com.finding.mate.dto.MateCreateDTO;
import com.finding.mate.entity.MateInvitation;
import com.finding.mate.entity.MateParticipant;
import com.finding.mate.mapper.MateInvitationMapper;
import com.finding.mate.mapper.MateParticipantMapper;
import com.finding.message.service.MessageService;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserService;
import com.finding.user.service.UserWriteGuard;
import com.finding.user.vo.UserVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    // ── P0-1 编辑活动接入统一内容审核 ──

    private MateCreateDTO dto(String title, String desc, String location) {
        MateCreateDTO d = new MateCreateDTO();
        d.setTitle(title);
        d.setDescription(desc);
        d.setLocation(location);
        return d;
    }

    private void stubReview(Set<String> blocking, Set<String> review) {
        when(sensitiveWordFilter.classifyReview(any(), any(), any()))
                .thenReturn(new ReviewResult(blocking, review));
    }

    @Test
    void updateInvitation_cleansBeforePersist() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        inv.setReviewStatus(0);
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        stubReview(Set.of(), Set.of()); // 无拦截无送审
        when(invitationMapper.updateById(any())).thenReturn(1);

        service.updateInvitation(100L, 1L, dto("<script>alert(1)</script>正常标题", "描述", "地点"));

        ArgumentCaptor<MateInvitation> cap = ArgumentCaptor.forClass(MateInvitation.class);
        verify(invitationMapper).updateById(cap.capture());
        assertFalse(cap.getValue().getTitle().contains("<script>"), "保存的必须是清洗后内容");
        assertEquals(0, cap.getValue().getReviewStatus()); // 无送审词 → 仍已发布
        verify(userWriteGuard).checkWritable(100L);
    }

    @Test
    void updateInvitation_blockingWord_rejects_withoutUpdate() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        stubReview(Set.of("违禁词"), Set.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateInvitation(100L, 1L, dto("标题", "内容含违禁词", "地点")));
        assertEquals(ResultCode.CONTENT_BLOCKED.getCode(), ex.getCode());
        verify(invitationMapper, never()).updateById(any()); // 数据库不变
    }

    @Test
    void updateInvitation_reviewWord_setsPending() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        inv.setReviewStatus(0);
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        stubReview(Set.of(), Set.of("送审词"));
        when(invitationMapper.updateById(any())).thenReturn(1);

        service.updateInvitation(100L, 1L, dto("标题", "内容含送审词", "地点"));

        ArgumentCaptor<MateInvitation> cap = ArgumentCaptor.forClass(MateInvitation.class);
        verify(invitationMapper).updateById(cap.capture());
        assertEquals(1, cap.getValue().getReviewStatus());
    }

    @Test
    void updateInvitation_rejected_thenEdit_resetsToPending() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        inv.setReviewStatus(2);
        inv.setReviewReason("旧原因");
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        stubReview(Set.of(), Set.of());
        when(invitationMapper.updateById(any())).thenReturn(1);

        service.updateInvitation(100L, 1L, dto("新标题", "新描述", "新地点"));

        ArgumentCaptor<MateInvitation> cap = ArgumentCaptor.forClass(MateInvitation.class);
        verify(invitationMapper).updateById(cap.capture());
        assertEquals(1, cap.getValue().getReviewStatus()); // 被拒后编辑 → 重新送审
        assertNull(cap.getValue().getReviewReason());
    }

    @Test
    void updateInvitation_nonCreator_rejected() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        when(invitationMapper.selectById(1L)).thenReturn(inv);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateInvitation(200L, 1L, dto("标题", "描述", "地点")));
        assertEquals(ResultCode.NOT_CREATOR.getCode(), ex.getCode());
    }

    // ── P0-2 匿名活动不泄露发起人 ID ──

    @Test
    void detail_anonymous_nonOwner_hidesUserId() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        inv.setIsAnonymous(1);
        inv.setReviewStatus(0);
        when(invitationMapper.selectById(1L)).thenReturn(inv);

        var vo = service.getInvitationDetail(1L, 999L);
        assertNull(vo.getUserId());
        assertNull(vo.getAuthor());
    }

    @Test
    void detail_anonymous_owner_keepsUserId() {
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        inv.setIsAnonymous(1);
        inv.setReviewStatus(0);
        when(invitationMapper.selectById(1L)).thenReturn(inv);
        when(userService.getUserProfile(eq(100L), eq(100L))).thenReturn(new UserVO());

        var vo = service.getInvitationDetail(1L, 100L);
        assertEquals(100L, vo.getUserId());
        org.junit.jupiter.api.Assertions.assertNotNull(vo.getAuthor());
    }

    @Test
    void listMyApplications_anonymous_hidesNickname() {
        MateParticipant part = participant(5L, 1L, 200L, MateParticipantStatus.PENDING.getCode());
        Page<MateParticipant> pg = new Page<>(1, 10);
        pg.setRecords(List.of(part));
        pg.setTotal(1);
        when(participantMapper.selectPage(any(), any())).thenReturn(pg);
        MateInvitation inv = invitation(1L, 3, 10);
        inv.setUserId(100L);
        inv.setIsAnonymous(1);
        when(invitationMapper.selectBatchIds(any())).thenReturn(List.of(inv));

        var result = service.listMyApplications(200L, 1, 10);
        assertEquals("匿名", result.getRecords().get(0).get("authorNickname"));
    }

    // ── P0-3 全局搜索复用公开可见性过滤 ──

    @Test
    void searchInvitations_appliesBlockedFilter() {
        when(relationshipService.blockedUserIds(9L)).thenReturn(Set.of(1L, 2L));
        when(invitationMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 10));

        var result = service.searchInvitations(9L, "测试", 1, 10);
        verify(relationshipService).blockedUserIds(9L); // 拉黑发起人被排除
        verify(invitationMapper).selectPage(any(), any());
        org.junit.jupiter.api.Assertions.assertTrue(result.getRecords().isEmpty());
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
