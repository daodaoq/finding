package com.finding.admin.service.impl;

import com.finding.admin.service.AdminUserLookupService;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员动态处置单测 —— 删除/下架需通知作者;审核 pass 缺省按拒绝处理。
 */
@ExtendWith(MockitoExtension.class)
class AdminPostServiceImplTest {

    @Mock private PostMapper postMapper;
    @Mock private AdminUserLookupService userLookupService;
    @Mock private MessageService messageService;
    @Mock private OperationAuditService operationAuditService;

    private AdminPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminPostServiceImpl(postMapper, userLookupService, messageService, operationAuditService);
    }

    @Test
    void deletePost_notifiesAuthor() {
        when(postMapper.selectById(1L)).thenReturn(post(1L, 55L, 1));

        service.deletePost(99L, 1L);

        verify(messageService).notify(eq(99L), eq(55L), eq("post_admin_action"), any(), eq(1L));
    }

    @Test
    void hidePost_notifiesAuthor() {
        when(postMapper.selectById(1L)).thenReturn(post(1L, 55L, 1));

        service.updatePostStatus(99L, 1L, Map.of("status", 2));

        verify(messageService).notify(eq(99L), eq(55L), eq("post_admin_action"), any(), eq(1L));
    }

    /** 回归:请求体缺 pass 时必须按「拒绝」处理,不能默认放行内容 */
    @Test
    void reviewPost_missingPass_defaultsToReject() {
        when(postMapper.selectById(1L)).thenReturn(post(1L, 55L, 1));

        Map<String, Object> body = new HashMap<>();
        body.put("reason", "内容不合规");
        service.reviewPost(99L, 1L, body);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getReviewStatus(), "缺省应视为拒绝(reviewStatus=2)");
        assertEquals(99L, captor.getValue().getReviewBy());
        verify(messageService).notify(eq(99L), eq(55L), eq("post_rejected"), any(), eq(1L));
    }

    @Test
    void reviewPost_passTrue_publishesWithoutNotify() {
        when(postMapper.selectById(1L)).thenReturn(post(1L, 55L, 1));

        service.reviewPost(99L, 1L, Map.of("pass", true));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getReviewStatus(), "通过应置 reviewStatus=0");
        verify(messageService, never()).notify(any(), any(), any(), any(), any());
    }

    private Post post(Long id, Long userId, Integer status) {
        Post p = new Post();
        p.setId(id);
        p.setUserId(userId);
        p.setStatus(status);
        return p;
    }
}
