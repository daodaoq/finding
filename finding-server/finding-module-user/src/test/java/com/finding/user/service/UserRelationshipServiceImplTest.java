package com.finding.user.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.user.entity.User;
import com.finding.user.entity.UserRemark;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserBlockMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserRemarkMapper;
import com.finding.user.service.impl.UserRelationshipServiceImpl;
import com.finding.user.vo.UserVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock
    private UserRemarkMapper userRemarkMapper;
    @Mock
    private RemarkCache remarkCache;

    private UserRelationshipServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 纯单测无 MyBatis 上下文:手动注册实体,使 LambdaQueryWrapper 能解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserRemark.class);
        service = new UserRelationshipServiceImpl(userMapper, userBlockMapper, userSettingsService,
                infoShareQuery, userRemarkMapper, remarkCache);
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

    // ── 备注(remarkOf / remarkMap / projectRemark)──

    @Test
    void remarkOf_selfOrNullViewer_returnsNull() {
        assertNull(service.remarkOf(1L, 1L));
        assertNull(service.remarkOf(null, 2L));
        assertNull(service.remarkOf(1L, null));
        verify(userRemarkMapper, never()).selectOne(any());
    }

    @Test
    void remarkOf_absent_returnsNullAndCachesNegative() {
        when(remarkCache.get(1L, 2L)).thenReturn(null);
        when(userRemarkMapper.selectOne(any())).thenReturn(null);
        assertNull(service.remarkOf(1L, 2L));
        verify(remarkCache).put(1L, 2L, "");     // 负缓存哨兵
    }

    @Test
    void remarkOf_present_returnsValue() {
        when(remarkCache.get(1L, 2L)).thenReturn(null);
        UserRemark row = new UserRemark();
        row.setRemark("小明");
        when(userRemarkMapper.selectOne(any())).thenReturn(row);
        assertEquals("小明", service.remarkOf(1L, 2L));
    }

    @Test
    void remarkOf_cacheHit_skipsDb() {
        when(remarkCache.get(1L, 2L)).thenReturn("小明");
        assertEquals("小明", service.remarkOf(1L, 2L));
        verify(userRemarkMapper, never()).selectOne(any());
    }

    @Test
    void remarkMap_excludesSelfAndBlanks() {
        UserRemark hit = new UserRemark();
        hit.setTargetUserId(2L);
        hit.setRemark("小明");
        UserRemark blank = new UserRemark();
        blank.setTargetUserId(3L);
        blank.setRemark("  ");
        when(userRemarkMapper.selectList(any())).thenReturn(List.of(hit, blank));

        Map<Long, String> map = service.remarkMap(1L, List.of(1L, 2L, 3L));
        assertEquals(Map.of(2L, "小明"), map);
    }

    @Test
    void remarkMap_emptyInputs_returnsEmpty() {
        assertTrue(service.remarkMap(null, List.of(2L)).isEmpty());
        assertTrue(service.remarkMap(1L, List.of()).isEmpty());
        assertTrue(service.remarkMap(1L, List.of(1L)).isEmpty());   // 只有自己
        verify(userRemarkMapper, never()).selectList(any());
    }

    @Test
    void projectRemark_replacesNicknameOnlyWhenPresent() {
        UserVO vo = new UserVO();
        vo.setNickname("张三");
        when(remarkCache.get(1L, 2L)).thenReturn(null);
        when(userRemarkMapper.selectOne(any())).thenReturn(null);
        service.projectRemark(1L, 2L, vo);
        assertEquals("张三", vo.getNickname(), "无备注时保持真实昵称");

        when(remarkCache.get(1L, 3L)).thenReturn("老板");
        service.projectRemark(1L, 3L, vo);
        assertEquals("老板", vo.getNickname(), "有备注时覆盖昵称");
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
