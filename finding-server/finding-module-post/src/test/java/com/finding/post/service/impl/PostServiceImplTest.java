package com.finding.post.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.ReviewResult;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.message.service.MessageService;
import com.finding.post.dto.PostCreateDTO;
import com.finding.post.entity.Post;
import com.finding.post.entity.PostComment;
import com.finding.post.mapper.PostCommentLikeMapper;
import com.finding.post.mapper.PostCommentMapper;
import com.finding.post.mapper.PostLikeMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 动态审核单测 —— 拦截词拒绝 / 送审词进入待审 / 干净直接发布 / 审核可见性 / 评论软删除。
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostMapper postMapper;
    @Mock private PostLikeMapper likeMapper;
    @Mock private PostCommentLikeMapper commentLikeMapper;
    @Mock private PostCommentMapper commentMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserFollowMapper followMapper;
    @Mock private UserService userService;
    @Mock private MessageService messageService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private PostServiceImpl service;

    @BeforeEach
    void initMybatisLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Post.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PostComment.class);
    }

    @Test
    void createPost_blockingWord_rejected() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of("坏词"), Set.of()));
        assertThrows(BusinessException.class, () -> service.createPost(1L, dto("包含坏词的内容")));
    }

    @Test
    void createPost_reviewWord_pendingReview() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of("送审词")));
        when(userService.getUserProfile(any(), any())).thenReturn(new com.finding.user.vo.UserVO());

        service.createPost(1L, dto("包含送审词的内容"));

        ArgumentCaptor<Post> cap = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(cap.capture());
        assertEquals(1, cap.getValue().getReviewStatus());
    }

    @Test
    void createPost_clean_published() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        when(userService.getUserProfile(any(), any())).thenReturn(new com.finding.user.vo.UserVO());

        service.createPost(1L, dto("干净内容"));

        ArgumentCaptor<Post> cap = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(cap.capture());
        assertEquals(0, cap.getValue().getReviewStatus());
    }

    @Test
    void getPostDetail_pending_othersNotFound() {
        Post post = post(1L, 1L, 1); // 作者=1,待审
        when(postMapper.selectById(1L)).thenReturn(post);
        // 他人(2)查看待审动态 → 视为不存在
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPostDetail(1L, 2L));
        assertEquals(ResultCode.POST_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getPostDetail_pending_authorSees() {
        Post post = post(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(commentMapper.selectCount(any())).thenReturn(0L);
        when(postMapper.updateById(any())).thenReturn(1);
        // 作者本人可查看
        service.getPostDetail(1L, 1L);
    }

    @Test
    void deleteComment_softDelete() {
        PostComment comment = new PostComment();
        comment.setId(10L);
        comment.setUserId(1L);
        comment.setStatus(0);
        when(commentMapper.selectById(10L)).thenReturn(comment);

        service.deleteComment(1L, 10L);

        ArgumentCaptor<PostComment> cap = ArgumentCaptor.forClass(PostComment.class);
        verify(commentMapper).updateById(cap.capture());
        assertEquals(1, cap.getValue().getStatus());
    }

    private PostCreateDTO dto(String content) {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setContent(content);
        return dto;
    }

    private Post post(Long id, Long userId, Integer reviewStatus) {
        Post p = new Post();
        p.setId(id);
        p.setUserId(userId);
        p.setContent("内容");
        p.setStatus(1);
        p.setReviewStatus(reviewStatus);
        p.setViewCount(0);
        p.setLikeCount(0);
        p.setCommentCount(0);
        return p;
    }
}
