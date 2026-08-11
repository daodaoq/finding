package com.finding.chat.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.chat.entity.InfoShare;
import com.finding.chat.mapper.InfoShareMapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.framework.util.InMemoryRateLimiter;
import com.finding.framework.websocket.WebSocketServer;
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
    @Mock private WebSocketServer webSocketServer;
    @Mock private VerificationGuard verificationGuard;
    @Mock private UserRelationshipService relationshipService;
    @Mock private InMemoryRateLimiter rateLimiter;

    @InjectMocks
    private InfoShareServiceImpl service;

    @BeforeEach
    void initMybatisLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), InfoShare.class);
    }

    private User activeUser() {
        User u = new User();
        u.setStatus(1);
        return u;
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
        when(infoShareMapper.selectOne(any())).thenReturn(null);
        when(infoShareMapper.insert(any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(webSocketServer.isOnline(2L)).thenReturn(false);

        assertDoesNotThrow(() -> service.requestShare(1L, 2L));
        verify(infoShareMapper).insert(any());
        verify(messageService).notify(any(), any(), any(), any(), any());
    }
}
