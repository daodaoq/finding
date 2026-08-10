package com.finding.user.service;

import com.finding.user.entity.User;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserBlockMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.impl.UserRelationshipServiceImpl;
import com.finding.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 关系权限服务单测 —— 纯 Mockito,不起 Spring 上下文、不连库。
 */
class UserRelationshipServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserBlockMapper userBlockMapper;
    @Mock
    private UserSettingsService userSettingsService;
    @Mock
    private InfoShareQuery infoShareQuery;

    private UserRelationshipServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserRelationshipServiceImpl(userMapper, userBlockMapper, userSettingsService, infoShareQuery);
    }

    // ── isBlockedEitherWay ──

    @Test
    void blockedEitherWay_anyDirection() {
        when(userBlockMapper.selectCount(any())).thenReturn(1L);
        assertTrue(service.isBlockedEitherWay(1L, 2L));
    }

    @Test
    void notBlocked() {
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        assertFalse(service.isBlockedEitherWay(1L, 2L));
    }

    @Test
    void blockedEitherWay_selfIsNotBlocked() {
        assertFalse(service.isBlockedEitherWay(1L, 1L));
    }

    // ── canViewDetailedProfile ──

    @Test
    void viewOwnProfile_alwaysDetailed() {
        assertTrue(service.canViewDetailedProfile(1L, 1L));
    }

    @Test
    void viewProfile_blockedEitherWay_publicOnly() {
        when(userBlockMapper.selectCount(any())).thenReturn(1L);
        assertFalse(service.canViewDetailedProfile(1L, 2L));
    }

    @Test
    void viewProfile_visibleToAll_detailed() {
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        UserSettings s = settings(1);
        when(userSettingsService.getSettings(2L)).thenReturn(s);
        assertTrue(service.canViewDetailedProfile(1L, 2L));
    }

    @Test
    void viewProfile_shareOnly_notShared_publicOnly() {
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        UserSettings s = settings(2);
        when(userSettingsService.getSettings(2L)).thenReturn(s);
        when(infoShareQuery.hasApprovedShare(1L, 2L)).thenReturn(false);
        assertFalse(service.canViewDetailedProfile(1L, 2L));
    }

    @Test
    void viewProfile_shareOnly_shared_detailed() {
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        UserSettings s = settings(2);
        when(userSettingsService.getSettings(2L)).thenReturn(s);
        when(infoShareQuery.hasApprovedShare(1L, 2L)).thenReturn(true);
        assertTrue(service.canViewDetailedProfile(1L, 2L));
    }

    // ── canDiscover ──

    @Test
    void discover_blocked_false() {
        when(userBlockMapper.selectCount(any())).thenReturn(1L);
        assertFalse(service.canDiscover(1L, 2L));
    }

    @Test
    void discover_notSearchable_false() {
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        User u = user(1);
        when(userMapper.selectById(2L)).thenReturn(u);
        when(userSettingsService.getSettings(2L)).thenReturn(settings(1, 0));
        assertFalse(service.canDiscover(1L, 2L));
    }

    @Test
    void discover_searchableAndActive_true() {
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        User u = user(1);
        when(userMapper.selectById(2L)).thenReturn(u);
        when(userSettingsService.getSettings(2L)).thenReturn(settings(1, 1));
        assertTrue(service.canDiscover(1L, 2L));
    }

    @Test
    void discover_self_false() {
        assertFalse(service.canDiscover(1L, 1L));
    }

    // ── projectDetailedFields ──

    @Test
    void project_hidesDetailedFields_whenNotShared() {
        when(userBlockMapper.selectCount(any())).thenReturn(0L);
        when(userSettingsService.getSettings(2L)).thenReturn(settings(2));
        when(infoShareQuery.hasApprovedShare(1L, 2L)).thenReturn(false);
        UserVO vo = new UserVO();
        vo.setGender(1);
        vo.setSignature("sig");
        vo.setCity("city");
        service.projectDetailedFields(1L, 2L, vo);
        assertNull(vo.getGender());
        assertNull(vo.getSignature());
        assertNull(vo.getCity());
    }

    private User user(int status) {
        User u = new User();
        u.setId(2L);
        u.setStatus(status);
        return u;
    }

    private UserSettings settings(int profileVisible) {
        return settings(profileVisible, 1);
    }

    private UserSettings settings(int profileVisible, int searchable) {
        UserSettings s = new UserSettings();
        s.setProfileVisible(profileVisible);
        s.setSearchable(searchable);
        return s;
    }
}
