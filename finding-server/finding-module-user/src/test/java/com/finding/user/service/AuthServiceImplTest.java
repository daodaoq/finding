package com.finding.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.RedisUtils;
import com.finding.common.ResultCode;
import com.finding.common.event.AccountDeletedEvent;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserVerificationMapper;
import com.finding.user.security.JwtTokenProvider;
import com.finding.user.service.impl.AuthServiceImpl;
import com.finding.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 账号注销单测 —— 密码校验 / 匿名化 / 状态停用 / 事件联动。 */
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserVerificationMapper verificationMapper;
    @Mock private UserFollowMapper followMapper;
    @Mock private UserPostStatsQuery userPostStatsQuery;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisUtils redisUtils;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private SensitiveWordFilter sensitiveWordFilter;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuthServiceImpl(userMapper, verificationMapper, followMapper, userPostStatsQuery,
                jwtTokenProvider, redisUtils, passwordEncoder, authenticationManager, sensitiveWordFilter, eventPublisher);
    }

    @Test
    void deleteAccount_anonymizesAndDisables() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800000002");
        user.setUsername("u2");
        user.setPassword("$2a$10$hash");
        user.setNickname("小美");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("12345678", "$2a$10$hash")).thenReturn(true);
        when(userMapper.updateById(any())).thenReturn(1);

        service.deleteAccount(1L, "12345678");

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(cap.capture());
        assertEquals(3, cap.getValue().getStatus());            // DELETED
        assertEquals("已注销用户", cap.getValue().getNickname());
        assertFalse(cap.getValue().getPhone().equals("13800000002")); // 手机号已匿名
        verify(eventPublisher).publishEvent(any(AccountDeletedEvent.class)); // 触发联动事件
    }

    @Test
    void deleteAccount_wrongPassword_rejected() {
        User user = new User();
        user.setId(1L);
        user.setPassword("$2a$10$hash");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteAccount(1L, "wrong"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(userMapper, never()).updateById(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /** 回归:getMe 需返回 targetType,否则编辑资料"交友目标"保存后重开仍显示未设置 */
    @Test
    void getCurrentUser_returnsTargetType() {
        User user = new User();
        user.setId(1L);
        user.setTargetType(1); // 找对象
        when(userMapper.selectById(1L)).thenReturn(user);
        when(followMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userPostStatsQuery.countPosts(anyLong())).thenReturn(0);

        UserVO vo = service.getCurrentUser(1L);

        assertEquals(1, vo.getTargetType());
    }
}
