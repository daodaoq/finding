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
import com.finding.post.vo.PostVO;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserService;
import com.finding.user.service.UserWriteGuard;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private SensitiveWordFilter sensitiveWordFilter;
    @Mock private UserWriteGuard userWriteGuard;

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
    void updatePost_frozenUser_rejected() {
        doThrow(new BusinessException(ResultCode.ACCOUNT_FROZEN)).when(userWriteGuard).checkWritable(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updatePost(1L, 1L, dto("新内容")));
        assertEquals(ResultCode.ACCOUNT_FROZEN.getCode(), ex.getCode());
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
        comment.setPostId(1L);
        comment.setUserId(1L);
        comment.setStatus(0);
        when(commentMapper.selectOne(any())).thenReturn(comment);

        service.deleteComment(1L, 1L, 10L); // userId, postId, commentId

        ArgumentCaptor<PostComment> cap = ArgumentCaptor.forClass(PostComment.class);
        verify(commentMapper).updateById(cap.capture());
        assertEquals(1, cap.getValue().getStatus());
    }

    @Test
    void deleteComment_crossPost_rejected() {
        // 评论属于其他动态 → 限定 postId 后查不到 → 不存在
        when(commentMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteComment(1L, 1L, 10L));
        assertEquals(ResultCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void toggleCommentLike_crossPost_rejected() {
        Post post = post(1L, 2L, 0); // 已发布,作者=2
        when(postMapper.selectById(1L)).thenReturn(post);
        when(commentMapper.selectOne(any())).thenReturn(null); // 评论不属于动态1

        BusinessException ex = assertThrows(BusinessException.class, () -> service.toggleCommentLike(1L, 1L, 10L));
        assertEquals(ResultCode.COMMENT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void addComment_crossParent_rejected() {
        Post post = post(1L, 2L, 0); // 已发布
        when(postMapper.selectById(1L)).thenReturn(post);
        when(commentMapper.selectOne(any())).thenReturn(null); // 父评论不属于动态1

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addComment(1L, 1L, 99L, "回复"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void listComments_invisiblePost_rejected() {
        Post post = post(1L, 1L, 1); // 待审,作者=1
        when(postMapper.selectById(1L)).thenReturn(post);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.listComments(1L, 1, 10, 2L)); // 他人查评论
        assertEquals(ResultCode.POST_NOT_FOUND.getCode(), ex.getCode());
    }

    // ── 3.10 图片约束 / 3.11 JSON存储 / 3.13 浏览量去重 ──

    @Test
    void createPost_tooManyImages_rejected() {
        List<String> imgs = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> "/api/v1/images/img" + i + ".jpg").toList();
        PostCreateDTO dto = dto("内容");
        dto.setImages(imgs);
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createPost(1L, dto));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
    }

    @Test
    void createPost_externalImageUrl_rejected() {
        PostCreateDTO dto = dto("内容");
        dto.setImages(List.of("https://evil.com/x.jpg"));
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createPost(1L, dto));
        assertEquals(ResultCode.PARAM_VALIDATION_FAILED.getCode(), ex.getCode());
    }

    @Test
    void createPost_storesImagesAsJson() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        when(userService.getUserProfile(any(), any())).thenReturn(new com.finding.user.vo.UserVO());
        when(likeMapper.selectCount(any())).thenReturn(0L);
        PostCreateDTO dto = dto("内容");
        // 含逗号的 URL 必须原样保存与回显
        dto.setImages(List.of("/api/v1/images/a,b.jpg", "/api/v1/images/c.jpg"));

        PostVO vo = service.createPost(1L, dto);

        ArgumentCaptor<Post> cap = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(cap.capture());
        assertTrue(cap.getValue().getImages().startsWith("["), "应存储为 JSON 数组");
        assertEquals(2, vo.getImages().size());
        assertEquals("/api/v1/images/a,b.jpg", vo.getImages().get(0));
    }

    @Test
    void getPostDetail_viewDeduped_noIncrement() {
        Post post = post(1L, 2L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(commentMapper.selectCount(any())).thenReturn(5L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(), any(), any())).thenReturn(false); // 已计过浏览

        service.getPostDetail(1L, 9L);

        verify(postMapper, never()).updateById(any()); // 去重后不累加浏览量
    }

    // ── 回归测试:禁言创建 / 已删动态评论 / 已删父评论 ──

    @Test
    void createPost_frozenUser_rejected() {
        doThrow(new BusinessException(ResultCode.ACCOUNT_FROZEN)).when(userWriteGuard).checkWritable(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createPost(1L, dto("内容")));
        assertEquals(ResultCode.ACCOUNT_FROZEN.getCode(), ex.getCode());
    }

    @Test
    void listComments_deletedPost_rejected() {
        Post post = post(1L, 1L, 0);
        post.setStatus(0); // 已删除
        when(postMapper.selectById(1L)).thenReturn(post);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.listComments(1L, 1, 10, 2L));
        assertEquals(ResultCode.POST_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void addComment_deletedParent_rejected() {
        Post post = post(1L, 2L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);
        PostComment parent = new PostComment();
        parent.setId(99L);
        parent.setPostId(1L);
        parent.setStatus(1); // 已删除
        when(commentMapper.selectOne(any())).thenReturn(parent);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addComment(1L, 1L, 99L, "回复"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void toggleCommentLike_deletedPost_rejected() {
        Post post = post(1L, 1L, 0);
        post.setStatus(0); // 已删除
        when(postMapper.selectById(1L)).thenReturn(post);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.toggleCommentLike(1L, 1L, 10L));
        assertEquals(ResultCode.POST_NOT_FOUND.getCode(), ex.getCode());
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
