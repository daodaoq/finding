package com.finding.bridge.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.bridge.entity.UserLike;
import com.finding.bridge.entity.UserMatch;
import com.finding.bridge.mapper.UserLikeMapper;
import com.finding.bridge.mapper.UserMatchMapper;
import com.finding.bridge.vo.MatchUserVO;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.message.service.MessageService;
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
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 双向心动配对单测 —— 喜欢/配对/取消/三类列表。 */
@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock private UserLikeMapper userLikeMapper;
    @Mock private UserMatchMapper userMatchMapper;
    @Mock private UserMapper userMapper;
    @Mock private MessageService messageService;
    @Mock private UserRelationshipService relationshipService;
    @InjectMocks private MatchServiceImpl service;

    @BeforeEach
    void initMybatisLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserLike.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserMatch.class);
    }

    private User activeUser(long id) {
        User u = new User();
        u.setId(id);
        u.setNickname("u" + id);
        u.setStatus(1);
        u.setRealNameVerified(2);
        return u;
    }

    @Test
    void likeUser_self_rejected() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.likeUser(1L, 1L));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void likeUser_targetNotFound_rejected() {
        when(userMapper.selectById(9L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.likeUser(1L, 9L));
    }

    @Test
    void likeUser_blocked_rejected() {
        when(userMapper.selectById(9L)).thenReturn(activeUser(9L));
        when(relationshipService.isBlockedEitherWay(1L, 9L)).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.likeUser(1L, 9L));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    @Test
    void likeUser_oneWay_returnsFalse_noNotify() {
        when(userMapper.selectById(9L)).thenReturn(activeUser(9L));
        when(userLikeMapper.insert(any())).thenReturn(1);
        when(userLikeMapper.selectCount(any())).thenReturn(0L); // 对方未喜欢我

        boolean matched = service.likeUser(1L, 9L);

        assertFalse(matched);
        verify(userMatchMapper, never()).insert(any());
        verify(messageService, never()).notify(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void likeUser_mutual_createsMatch_notifiesBoth() {
        when(userMapper.selectById(9L)).thenReturn(activeUser(9L));
        when(userMapper.selectById(1L)).thenReturn(activeUser(1L));
        when(userLikeMapper.insert(any())).thenReturn(1);
        when(userLikeMapper.selectCount(any())).thenReturn(1L); // 对方也喜欢我
        when(userMatchMapper.selectCount(any())).thenReturn(0L); // 尚无配对
        when(userMatchMapper.insert(any())).thenReturn(1);

        boolean matched = service.likeUser(1L, 9L);

        assertTrue(matched);
        verify(userMatchMapper).insert(any());
        verify(messageService, times(2)).notify(anyLong(), anyLong(), eq("match"), any(), any());
    }

    @Test
    void likeUser_alreadyLikedAndMatched_idempotent_noDuplicateNotify() {
        when(userMapper.selectById(9L)).thenReturn(activeUser(9L));
        when(userLikeMapper.insert(any())).thenThrow(new DuplicateKeyException("dup"));
        when(userLikeMapper.selectCount(any())).thenReturn(1L); // 反向喜欢存在
        when(userMatchMapper.selectCount(any())).thenReturn(1L); // 配对已存在

        boolean matched = service.likeUser(1L, 9L);

        assertTrue(matched);
        verify(userMatchMapper, never()).insert(any()); // 不重复插配对
        verify(messageService, never()).notify(anyLong(), anyLong(), any(), any(), any()); // 不重复通知
    }

    @Test
    void unlikeUser_removesLikeAndMatch() {
        service.unlikeUser(1L, 9L);
        verify(userLikeMapper).delete(any());
        verify(userMatchMapper).delete(any());
    }

    @Test
    void getMyLikes_marksMutualOnly() {
        Page<UserLike> page = new Page<>(1, 20);
        UserLike l1 = new UserLike(); l1.setLikerId(1L); l1.setLikedId(10L); l1.setCreatedAt(LocalDateTime.now());
        UserLike l2 = new UserLike(); l2.setLikerId(1L); l2.setLikedId(20L); l2.setCreatedAt(LocalDateTime.now());
        page.setRecords(List.of(l1, l2));
        when(userLikeMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(activeUser(10L), activeUser(20L)));
        // 只有 20 也喜欢我
        UserLike back = new UserLike(); back.setLikerId(20L); back.setLikedId(1L);
        when(userLikeMapper.selectList(any())).thenReturn(List.of(back));

        PageVO<MatchUserVO> vo = service.getMyLikes(1L, 1, 20);

        assertEquals(2, vo.getRecords().size());
        assertEquals(10L, vo.getRecords().get(0).getUserId());
        assertFalse(vo.getRecords().get(0).getIsMatched());
        assertEquals(20L, vo.getRecords().get(1).getUserId());
        assertTrue(vo.getRecords().get(1).getIsMatched());
    }

    @Test
    void getLikesReceived_marksMutualOnly() {
        Page<UserLike> page = new Page<>(1, 20);
        UserLike l1 = new UserLike(); l1.setLikerId(10L); l1.setLikedId(1L); l1.setCreatedAt(LocalDateTime.now());
        page.setRecords(List.of(l1));
        when(userLikeMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(activeUser(10L)));
        // 我也喜欢 10
        UserLike back = new UserLike(); back.setLikerId(1L); back.setLikedId(10L);
        when(userLikeMapper.selectList(any())).thenReturn(List.of(back));

        PageVO<MatchUserVO> vo = service.getLikesReceived(1L, 1, 20);

        assertEquals(1, vo.getRecords().size());
        assertTrue(vo.getRecords().get(0).getIsMatched());
    }

    @Test
    void getMyMatches_allMatched() {
        Page<UserMatch> page = new Page<>(1, 20);
        UserMatch m = new UserMatch(); m.setUserAId(1L); m.setUserBId(10L); m.setMatchedAt(LocalDateTime.now());
        page.setRecords(List.of(m));
        when(userMatchMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(activeUser(10L)));

        PageVO<MatchUserVO> vo = service.getMyMatches(1L, 1, 20);

        assertEquals(1, vo.getRecords().size());
        assertEquals(10L, vo.getRecords().get(0).getUserId());
        assertTrue(vo.getRecords().get(0).getIsMatched());
    }
}
