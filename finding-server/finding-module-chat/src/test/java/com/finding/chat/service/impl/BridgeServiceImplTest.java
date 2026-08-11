package com.finding.chat.service.impl;

import com.finding.chat.config.MatchScoreWeights;
import com.finding.chat.dto.UserCardConfigDTO;
import com.finding.chat.entity.ChatApply;
import com.finding.chat.entity.RecommendEvent;
import com.finding.chat.entity.UserCardConfig;
import com.finding.chat.entity.UserMatchPreference;
import com.finding.chat.mapper.ChatApplyMapper;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RecommendEventMapper;
import com.finding.chat.mapper.RecommendExcludeMapper;
import com.finding.chat.mapper.RoomMapper;
import com.finding.chat.mapper.UserMatchPreferenceMapper;
import com.finding.chat.service.ChatService;
import com.finding.chat.vo.HomeFeedVO;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.framework.util.InMemoryRateLimiter;
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
import com.finding.user.service.UserWriteGuard;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
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
    @Mock private com.finding.chat.mapper.UserCardConfigMapper cardConfigMapper;
    @Mock private MatchScoreWeights weights;
    @Mock private UserWriteGuard userWriteGuard;
    @Mock private InMemoryRateLimiter rateLimiter;

    @InjectMocks
    private BridgeServiceImpl service;

    /** 放行反骚扰限流(mock 默认 false 会让所有申请被限流) */
    private void allowRateLimit() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
    }

    @BeforeEach
    void initMybatisLambdaCache() {
        // 纯单测无 Spring 上下文:注册实体 TableInfo,使 LambdaQuery/LambdaUpdateWrapper 可解析列名
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ChatApply.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserSettings.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserMatchPreference.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), com.finding.chat.entity.RecommendExclude.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), com.finding.chat.entity.UserCardConfig.class);
    }

    private UserSettings settings(int friendAddMode) {
        UserSettings s = new UserSettings();
        s.setFriendAddMode(friendAddMode);
        return s;
    }

    private User activeUser() {
        User u = new User();
        u.setStatus(1);
        return u;
    }

    // ── applyChat: 冷却期 ──

    @Test
    void applyChat_recentRejection_blocksReapply() {
        allowRateLimit();
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(relationshipService.canApplyChat(1L, 2L)).thenReturn(true);
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
        allowRateLimit();
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(relationshipService.canApplyChat(1L, 2L)).thenReturn(true);
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
        allowRateLimit();
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(relationshipService.canApplyChat(1L, 2L)).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(userSettingsMapper.selectOne(any())).thenReturn(settings(1));
        when(chatApplyMapper.selectOne(any())).thenReturn(null);
        when(chatApplyMapper.selectCount(any())).thenReturn(1L); // 已存在待处理

        BusinessException ex = assertThrows(BusinessException.class, () -> service.applyChat(1L, 2L, "hi"));
        assertEquals(ResultCode.CHAT_APPLY_ALREADY_SENT.getCode(), ex.getCode());
    }

    // ── applyChat: 统一发现/申请权限 ──

    @Test
    void applyChat_targetNotDiscoverable_throws() {
        allowRateLimit();
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(false);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.applyChat(1L, 2L, "hi"));
        assertEquals(ResultCode.USER_NOT_DISCOVERABLE.getCode(), ex.getCode());
    }

    @Test
    void applyChat_targetBlocked_throws() {
        allowRateLimit();
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(false);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.applyChat(1L, 2L, "hi"));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    @Test
    void applyChat_friendAddModeNotAllowed_throws() {
        allowRateLimit();
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(relationshipService.canApplyChat(1L, 2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.applyChat(1L, 2L, "hi"));
        assertEquals(ResultCode.CONTACT_PERMISSION_DENIED.getCode(), ex.getCode());
    }

    // ── handleApply: 条件更新防并发 ──

    @Test
    void handleApply_conditionalUpdateFails_throwsAlreadyHandled() {
        ChatApply apply = pending(10L, 2L, 1L);
        when(chatApplyMapper.selectById(10L)).thenReturn(apply);
        when(relationshipService.isBlockedEitherWay(2L, 1L)).thenReturn(false);
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(userMapper.selectById(2L)).thenReturn(activeUser());
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
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(userMapper.selectById(2L)).thenReturn(activeUser());
        when(chatApplyMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.handleApply(1L, 10L, 2)); // 2=拒绝
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    @Test
    void handleApply_anyPartyInactive_cancelsAndThrows() {
        ChatApply apply = pending(10L, 2L, 1L);
        when(chatApplyMapper.selectById(10L)).thenReturn(apply);
        when(relationshipService.isBlockedEitherWay(2L, 1L)).thenReturn(false);
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(userMapper.selectById(2L)).thenReturn(new User()); // 申请人账号状态异常(status=null)

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handleApply(1L, 10L, 1));
        assertEquals(ResultCode.USER_NOT_DISCOVERABLE.getCode(), ex.getCode());
        // 待处理申请被置为已撤回(CANCELLED)(另有 expireStalePending 的一次批量更新)
        verify(chatApplyMapper, atLeastOnce()).update(any(), any());
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
    void updateMatchPreference_invalidMaxDistance_rejected() {
        UserMatchPreference pref = new UserMatchPreference();
        pref.setMaxDistanceKm(-1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateMatchPreference(1L, pref));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
    }

    @Test
    void updateMatchPreference_invalidOnlyVerified_rejected() {
        UserMatchPreference pref = new UserMatchPreference();
        pref.setOnlyVerified(2);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateMatchPreference(1L, pref));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
    }

    @Test
    void updateMatchPreference_preferCityTrimmed() {
        UserMatchPreference pref = new UserMatchPreference();
        pref.setPreferCity("  北京  ");
        when(preferenceMapper.selectOne(any())).thenReturn(null);
        when(preferenceMapper.insert(any())).thenReturn(1);

        service.updateMatchPreference(1L, pref);

        ArgumentCaptor<UserMatchPreference> captor = ArgumentCaptor.forClass(UserMatchPreference.class);
        verify(preferenceMapper).insert(captor.capture());
        assertEquals("北京", captor.getValue().getPreferCity());
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

    @Test
    void skipUser_duplicateEventToday_ignored() {
        // dedup_key 唯一约束触发 DuplicateKeyException → 幂等忽略,不抛异常
        when(excludeMapper.selectCount(any())).thenReturn(1L);
        when(eventMapper.insert(any())).thenThrow(new DuplicateKeyException("dedup_key"));

        assertDoesNotThrow(() -> service.skipUser(1L, 2L));
    }

    // ── 相识卡片配置 ──

    @Test
    void getCardConfig_noRow_returnsDefaults() {
        when(cardConfigMapper.selectOne(any())).thenReturn(null);
        UserCardConfig cfg = service.getCardConfig(1L);
        assertEquals(1, cfg.getShowPhoto());
        assertEquals(1, cfg.getShowNickname());
    }

    @Test
    void updateCardConfig_insertsWhenNone() {
        when(cardConfigMapper.selectOne(any())).thenReturn(null);
        when(cardConfigMapper.insert(any())).thenReturn(1);
        UserCardConfigDTO dto = new UserCardConfigDTO();
        dto.setShowNickname(0);
        service.updateCardConfig(1L, dto);

        ArgumentCaptor<UserCardConfig> captor = ArgumentCaptor.forClass(UserCardConfig.class);
        verify(cardConfigMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getShowNickname());
    }

    @Test
    void previewMyCard_appliesCardConfig() {
        User me = new User();
        me.setId(1L);
        me.setNickname("测试");
        me.setAvatar("a.jpg");
        me.setGender(1);
        me.setSchool("山东理工大学");
        me.setCity("淄博");
        when(userMapper.selectById(1L)).thenReturn(me);
        UserCardConfig cfg = new UserCardConfig();
        cfg.setShowNickname(0); // 隐藏昵称
        when(cardConfigMapper.selectOne(any())).thenReturn(cfg);

        HomeFeedVO vo = service.previewMyCard(1L);

        assertNull(vo.getNickname());
        assertEquals("a.jpg", vo.getAvatar());
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
