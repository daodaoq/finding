package com.finding.post.service.impl;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.post.entity.Appeal;
import com.finding.post.entity.Post;
import com.finding.post.mapper.AppealMapper;
import com.finding.post.mapper.PostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 申诉闭环:资格(被拒/下架)、次数上限、去重、落库字段。
 */
@ExtendWith(MockitoExtension.class)
class AppealServiceImplTest {

    @Mock
    private AppealMapper appealMapper;
    @Mock
    private PostMapper postMapper;

    private AppealServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppealServiceImpl(appealMapper, postMapper);
    }

    @Test
    void blankReason_throws() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.appeal(1L, 10L, "  "));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), e.getCode());
    }

    @Test
    void nullPost_throwsPostNotFound() {
        when(postMapper.selectById(10L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.appeal(1L, 10L, "理由"));
    }

    @Test
    void deletedPost_throwsPostNotFound() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, 1L, 0, 1, null));
        assertThrows(BusinessException.class, () -> service.appeal(1L, 10L, "理由"));
    }

    @Test
    void notOwnPost_throws() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, 999L, 2, 1, "违规"));
        assertThrows(BusinessException.class, () -> service.appeal(1L, 10L, "理由"));
    }

    @Test
    void publishedPost_throws() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, 1L, 0, 1, null));
        assertThrows(BusinessException.class, () -> service.appeal(1L, 10L, "理由"));
    }

    @Test
    void rejectedPost_inserts() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, 1L, 2, 1, "含违规词"));
        when(appealMapper.selectCount(any())).thenReturn(0L);

        service.appeal(1L, 10L, "我没有违规");

        ArgumentCaptor<Appeal> cap = ArgumentCaptor.forClass(Appeal.class);
        verify(appealMapper).insert(cap.capture());
        assertEquals("post", cap.getValue().getTargetType());
        assertEquals(10L, cap.getValue().getTargetId());
        assertEquals("含违规词", cap.getValue().getOriginalResult());
        assertEquals(0, cap.getValue().getStatus());
    }

    @Test
    void hiddenPost_inserts() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, 1L, 0, 2, null));
        when(appealMapper.selectCount(any())).thenReturn(0L);

        service.appeal(1L, 10L, "请恢复我的动态");

        ArgumentCaptor<Appeal> cap = ArgumentCaptor.forClass(Appeal.class);
        verify(appealMapper).insert(cap.capture());
        assertEquals("内容已下架", cap.getValue().getOriginalResult());
    }

    @Test
    void pendingDuplicate_throws() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, 1L, 2, 1, "违规"));
        when(appealMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.appeal(1L, 10L, "理由"));
        verify(appealMapper, never()).insert(any(Appeal.class));
    }

    @Test
    void exceedsAppealLimit_throws() {
        when(postMapper.selectById(10L)).thenReturn(post(10L, 1L, 2, 1, "违规"));
        // 第一次 count=待处理查(0),第二次 count=总数查(3)
        when(appealMapper.selectCount(any())).thenReturn(0L, 3L);

        assertThrows(BusinessException.class, () -> service.appeal(1L, 10L, "理由"));
        verify(appealMapper, never()).insert(any(Appeal.class));
    }

    private Post post(Long id, Long userId, int reviewStatus, int status, String reviewReason) {
        Post p = new Post();
        p.setId(id);
        p.setUserId(userId);
        p.setReviewStatus(reviewStatus);
        p.setStatus(status);
        p.setReviewReason(reviewReason);
        return p;
    }
}
