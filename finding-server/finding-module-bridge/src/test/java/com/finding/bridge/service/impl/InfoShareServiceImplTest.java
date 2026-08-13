package com.finding.bridge.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.bridge.entity.ChatApply;
import com.finding.bridge.entity.InfoShare;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.RoomFriend;
import com.finding.bridge.mapper.ChatApplyMapper;
import com.finding.bridge.mapper.InfoShareMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RoomFriendMapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.framework.util.RedisRateLimiter;
import com.finding.message.service.MessageService;
import com.finding.user.common.VerificationGuard;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserRelationshipService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 信息互换申请单测 —— 权限/限流 / 审批条件更新防并发 / 拉黑联动。
 */
@ExtendWith(MockitoExtension.class)
class InfoShareServiceImplTest {

    @Mock private InfoShareMapper infoShareMapper;
    @Mock private UserMapper userMapper;
    @Mock private MessageService messageService;
    @Mock private VerificationGuard verificationGuard;
    @Mock private UserRelationshipService relationshipService;
    @Mock private RedisRateLimiter rateLimiter;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ChatApplyMapper chatApplyMapper;
    @Mock private RoomFriendMapper roomFriendMapper;
    @Mock private PrivateChatMapper privateChatMapper;

    @InjectMocks
    private InfoShareServiceImpl service;

    @BeforeEach
    void initMybatisLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), InfoShare.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ChatApply.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RoomFriend.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PrivateChat.class);
    }

    private User activeUser() {
        User u = new User();
        u.setStatus(1);
        return u;
    }

    private InfoShare pendingShare(Long id, Long from, Long to) {
        InfoShare s = new InfoShare();
        s.setId(id);
        s.setFromUserId(from);
        s.setToUserId(to);
        s.setStatus(0);
        return s;
    }

    // ── requestShare: 限流 ──

    @Test
    void requestShare_rateLimited_throws() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requestShare(1L, 2L));
        assertEquals(ResultCode.TOO_FREQUENT.getCode(), ex.getCode());
        verify(infoShareMapper, never()).insert(any());
    }

    // ── requestShare: 发现权限 ──

    @Test
    void requestShare_targetBlocked_throws() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(false);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requestShare(1L, 2L));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    @Test
    void requestShare_targetNotDiscoverable_throws() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(false);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requestShare(1L, 2L));
        assertEquals(ResultCode.USER_NOT_DISCOVERABLE.getCode(), ex.getCode());
    }

    @Test
    void requestShare_success_insertsAndNotifies() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(activeUser());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(chatApplyMapper.selectCount(any())).thenReturn(1L); // 已通过聊天申请
        when(infoShareMapper.selectOne(any())).thenReturn(null);
        when(infoShareMapper.insert(any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(activeUser());

        assertDoesNotThrow(() -> service.requestShare(1L, 2L));
        verify(infoShareMapper).insert(any());
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    @Test
    void requestShare_withoutChatRelationship_throws() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(activeUser());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(chatApplyMapper.selectCount(any())).thenReturn(0L); // 无已通过的聊天申请
        when(roomFriendMapper.selectCount(any())).thenReturn(0L); // 也无私聊会话

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requestShare(1L, 2L));
        assertEquals(ResultCode.INFO_SHARE_NEED_CHAT.getCode(), ex.getCode());
        verify(infoShareMapper, never()).insert(any());
    }

    /** 回归:已通过"打招呼"建立会话、但没走过心动申请的用户,应允许互换信息 */
    @Test
    void requestShare_withConversationButNoApply_succeeds() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(activeUser());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(chatApplyMapper.selectCount(any())).thenReturn(0L); // 无心动申请
        when(roomFriendMapper.selectCount(any())).thenReturn(1L); // 但已有会话(打招呼已接受)
        when(infoShareMapper.selectOne(any())).thenReturn(null);
        when(infoShareMapper.insert(any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(activeUser());

        assertDoesNotThrow(() -> service.requestShare(1L, 2L));
        verify(infoShareMapper).insert(any());
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    /** 回归:无会话/无心动申请,但确有私信往来的用户,应允许互换信息(兜底) */
    @Test
    void requestShare_withPrivateMessages_succeeds() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(activeUser());
        when(relationshipService.canDiscover(1L, 2L)).thenReturn(true);
        when(chatApplyMapper.selectCount(any())).thenReturn(0L);
        when(roomFriendMapper.selectCount(any())).thenReturn(0L);
        when(privateChatMapper.selectCount(any())).thenReturn(3L); // 有历史私信
        when(infoShareMapper.selectOne(any())).thenReturn(null);
        when(infoShareMapper.insert(any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(activeUser());

        assertDoesNotThrow(() -> service.requestShare(1L, 2L));
        verify(infoShareMapper).insert(any());
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    // ── handleShare: 条件更新防并发 ──

    @Test
    void handleShare_conditionalUpdate_success() {
        InfoShare share = pendingShare(100L, 2L, 1L);
        when(infoShareMapper.selectById(100L)).thenReturn(share);
        when(infoShareMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.handleShare(1L, 100L, 1));
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    @Test
    void handleShare_conditionalUpdateFails_throwsAlreadyHandled() {
        InfoShare share = pendingShare(100L, 2L, 1L);
        when(infoShareMapper.selectById(100L)).thenReturn(share);
        // 并发下条件更新返回 0 → 已被他人处理
        when(infoShareMapper.update(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handleShare(1L, 100L, 1));
        assertEquals(ResultCode.CHAT_APPLY_ALREADY_HANDLED.getCode(), ex.getCode());
        // 不产生任何通知
        verify(messageService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    void handleShare_notReceiver_throws() {
        InfoShare share = pendingShare(100L, 1L, 2L); // 接收方是 2,当前用户是 1
        when(infoShareMapper.selectById(100L)).thenReturn(share);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handleShare(1L, 100L, 1));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }
}
