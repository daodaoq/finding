package com.finding.admin.controller;

import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员动态处置单测 —— 删除/下架需通知作者。
 */
@ExtendWith(MockitoExtension.class)
class AdminPostControllerTest {

    @Mock private PostMapper postMapper;
    @Mock private UserMapper userMapper;
    @Mock private MessageService messageService;
    @Mock private OperationAuditService operationAuditService;
    @InjectMocks private AdminPostController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deletePost_notifiesAuthor() {
        setAdmin(99L);
        Post post = post(1L, 55L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        controller.deletePost(1L);

        verify(messageService).notify(eq(99L), eq(55L), eq("post_admin_action"), any(), eq(1L));
    }

    @Test
    void hidePost_notifiesAuthor() {
        setAdmin(99L);
        Post post = post(1L, 55L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        controller.updatePostStatus(1L, Map.of("status", 2));

        verify(messageService).notify(eq(99L), eq(55L), eq("post_admin_action"), any(), eq(1L));
    }

    private void setAdmin(Long id) {
        UserPrincipal principal = new UserPrincipal(id, String.valueOf(id));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private Post post(Long id, Long userId, Integer status) {
        Post p = new Post();
        p.setId(id);
        p.setUserId(userId);
        p.setStatus(status);
        return p;
    }
}
