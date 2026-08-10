package com.finding.user.service;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 关注关系单测 —— 被拉黑时服务端拒绝关注。
 */
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserFollowMapper followMapper;
    @Mock
    private UserPostStatsQuery userPostStatsQuery;
    @Mock
    private UserSettingsMapper userSettingsMapper;
    @Mock
    private UserRelationshipService relationshipService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserServiceImpl(userMapper, followMapper, userPostStatsQuery, userSettingsMapper, relationshipService);
    }

    @Test
    void followSelf_rejected() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.followUser(1L, 1L));
        assertEquals(ResultCode.CANNOT_FOLLOW_SELF.getCode(), ex.getCode());
    }

    @Test
    void followNonexistent_rejected() {
        when(userMapper.selectById(2L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.followUser(1L, 2L));
        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void followBlockedRelation_rejected() {
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(relationshipService.canFollow(1L, 2L)).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.followUser(1L, 2L));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    @Test
    void followAllowed_proceeds() {
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(relationshipService.canFollow(1L, 2L)).thenReturn(true);
        when(followMapper.selectOne(any())).thenReturn(null);
        // 不抛异常即通过
        service.followUser(1L, 2L);
    }
}
