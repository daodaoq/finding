package com.finding.chat.service.impl;

import com.finding.chat.config.MatchScoreWeights;
import com.finding.chat.entity.ChatApply;
import com.finding.chat.entity.RecommendEvent;
import com.finding.chat.entity.UserMatchPreference;
import com.finding.chat.mapper.ChatApplyMapper;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RecommendEventMapper;
import com.finding.chat.mapper.RecommendExcludeMapper;
import com.finding.chat.mapper.RoomMapper;
import com.finding.chat.mapper.UserMatchPreferenceMapper;
import com.finding.chat.service.ChatService;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.message.service.MessageService;
import com.finding.user.common.VerificationGuard;
import com.finding.user.entity.User;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.user.entity.UserSettings;
import com.finding.user.service.UserRelationshipService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 聊天申请状态机单测 —— 冷却期 / 条件更新防并发 / 撤回 / 惰性过期。
 */
@ExtendWith(MockitoExtension.class)
class BridgeServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private ChatApplyMapper chatApplyMapper;
    @Mock private UserFollowMapper followMapper;
    @Mock private MessageService messageService;
    @Mock private RoomMapper roomMapper;
    @Mock private PrivateChatMapper privateChatMapper;
    @Mock private ContactMapper contactMapper;
    @Mock private ChatService chatService;
    @Mock private VerificationGuard verificationGuard;
    @Mock private UserSettingsMapper userSettingsMapper;
    @Mock private SensitiveWordFilter sensitiveWordFilter;
    @Mock private UserRelationshipService relationshipService;
    @Mock private UserMatchPreferenceMapper preferenceMapper;
    @Mock private RecommendExcludeMapper excludeMapper;
    @Mock private RecommendEventMapper eventMapper;
    @Mock private MatchScoreWeights weights;

    @InjectMocks
    private BridgeServiceImpl service;

    @BeforeEach
    void initMybatisLambdaCache() {
        // 纯单测无 Spring 上下文:注册实体 TableInfo,使 LambdaQuery/LambdaUpdateWrapper 可解析列名
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ChatApply.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserSettings.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserMatchPreference.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), com.finding.chat.entity.RecommendExclude.class);
    }

    private UserSettings settings(int friendAddMode) {
        UserSettings s = new UserSettings();
        s.setFriendAddMode(friendAddMode);
        return s;
    }

    // ── applyChat: 冷却期 ──

    @Test
    void applyChat_recentRejection_blocksReapply() {
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(userSettingsMapper.selectOne(any())).thenReturn(settings(1));
        ChatApply rejected = new ChatApply();
        rejected.setStatus(2);
        rejected.setHandleTime(LocalDateTime.now().minusDays(1));
        when(chatApplyMapper.selectOne(any())).thenReturn(rejected); // 冷却期最近一条被拒绝

        BusinessException ex = assertThrows(BusinessException.class, () -> service.applyChat(1L, 2L, "hi"));
        assertEquals(ResultCode.CHAT_APPLY_COOLDOWN.getCode(), ex.getCode());
    }

    @Test
    void applyChat_noRecentRejection_insertsAndNotifies() {
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(userSettingsMapper.selectOne(any())).thenReturn(settings(1)); // 需验证
        when(chatApplyMapper.selectOne(any())).thenReturn(null); // 无冷却记录
        when(chatApplyMapper.selectCount(any())).thenReturn(0L);  // 无待处理
        when(chatApplyMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> service.applyChat(1L, 2L, "hi"));
        verify(chatApplyMapper).insert(any());
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    @Test
    void applyChat_pendingExists_blocksDuplicate() {
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(userSettingsMapper.selectOne(any())).thenReturn(settings(1));
        when(chatApplyMapper.selectOne(any())).thenReturn(null);
        when(chatApplyMapper.selectCount(any())).thenReturn(1L); // 已存在待处理

        BusinessException ex = assertThrows(BusinessException.class, () -> service.applyChat(1L, 2L, "hi"));
        assertEquals(ResultCode.CHAT_APPLY_ALREADY_SENT.getCode(), ex.getCode());
    }

    // ── handleApply: 条件更新防并发 ──

    @Test
    void handleApply_conditionalUpdateFails_throwsAlreadyHandled() {
        ChatApply apply = pending(10L, 2L, 1L);
        when(chatApplyMapper.selectById(10L)).thenReturn(apply);
        when(relationshipService.isBlockedEitherWay(2L, 1L)).thenReturn(false);
        // expireStalePending(update=0,无碍) + 条件更新(update=0 → 已被并发处理)
        when(chatApplyMapper.update(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handleApply(1L, 10L, 1));
        assertEquals(ResultCode.CHAT_APPLY_ALREADY_HANDLED.getCode(), ex.getCode());
    }

    @Test
    void handleApply_stalePending_isExpired() {
        ChatApply apply = pending(10L, 2L, 1L);
        apply.setApplyTime(LocalDateTime.now().minusDays(8)); // 超期
        when(chatApplyMapper.selectById(10L)).thenReturn(apply);
        when(chatApplyMapper.update(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handleApply(1L, 10L, 1));
        assertEquals(ResultCode.CHAT_APPLY_ALREADY_HANDLED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void handleApply_rejected_notifiesApplicant() {
        ChatApply apply = pending(10L, 2L, 1L);
        when(chatApplyMapper.selectById(10L)).thenReturn(apply);
        when(relationshipService.isBlockedEitherWay(2L, 1L)).thenReturn(false);
        when(chatApplyMapper.update(any(), any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(new User());

        assertDoesNotThrow(() -> service.handleApply(1L, 10L, 2)); // 2=拒绝
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    // ── withdrawApply ──

    @Test
    void withdrawApply_pending_succeeds() {
        ChatApply apply = new ChatApply();
        apply.setId(10L);
        apply.setFromUserId(1L);
        apply.setToUserId(2L);
        apply.setStatus(0);
        apply.setApplyTime(LocalDateTime.now());
        when(chatApplyMapper.selectById(10L)).thenReturn(apply);
        when(chatApplyMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.withdrawApply(1L, 10L));
    }

    @Test
    void withdrawApply_alreadyHandled_throws() {
        ChatApply apply = new ChatApply();
        apply.setId(10L);
        apply.setFromUserId(1L);
        apply.setToUserId(2L);
        apply.setStatus(0);
        apply.setApplyTime(LocalDateTime.now());
        when(chatApplyMapper.selectById(10L)).thenReturn(apply);
        when(chatApplyMapper.update(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.withdrawApply(1L, 10L));
        assertEquals(ResultCode.CHAT_APPLY_ALREADY_HANDLED.getCode(), ex.getCode());
    }

    // ── 相亲交友偏好 ──

    @Test
    void getMatchPreference_noRow_returnsDefaults() {
        when(preferenceMapper.selectOne(any())).thenReturn(null);
        UserMatchPreference p = service.getMatchPreference(1L);
        assertEquals(0, p.getPreferGender());
        assertEquals(0, p.getMaxDistanceKm());
    }

    @Test
    void updateMatchPreference_invalidGender_rejected() {
        UserMatchPreference pref = new UserMatchPreference();
        pref.setPreferGender(5);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateMatchPreference(1L, pref));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
    }

    @Test
    void skipUser_recordsExcludeAndEvent() {
        when(excludeMapper.selectCount(any())).thenReturn(0L);
        when(excludeMapper.insert(any())).thenReturn(1);
        when(eventMapper.insert(any())).thenReturn(1);

        service.skipUser(1L, 2L);

        verify(excludeMapper).insert(any());
        verify(eventMapper).insert(any());
    }

    @Test
    void skipUser_alreadyExcluded_skipsDuplicate() {
        when(excludeMapper.selectCount(any())).thenReturn(1L);
        when(eventMapper.insert(any())).thenReturn(1);
        service.skipUser(1L, 2L);
        // 不重复插入排除记录,但仍记录跳过事件
        verify(excludeMapper, never()).insert(any());
        verify(eventMapper).insert(any());
    }

    private ChatApply pending(Long id, Long from, Long to) {
        ChatApply a = new ChatApply();
        a.setId(id);
        a.setFromUserId(from);
        a.setToUserId(to);
        a.setStatus(0);
        a.setApplyTime(LocalDateTime.now());
        return a;
    }
}
