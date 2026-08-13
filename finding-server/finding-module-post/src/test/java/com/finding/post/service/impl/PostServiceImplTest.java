package com.finding.post.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.ReviewResult;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.message.service.MessageService;
import com.finding.post.dto.PostCreateDTO;
import com.finding.post.dto.PostDraftSaveDTO;
import com.finding.post.dto.PostQueryDTO;
import com.finding.post.entity.Post;
import com.finding.post.entity.PostComment;
import com.finding.post.entity.PostDraft;
import com.finding.post.entity.PostFavorite;
import com.finding.post.mapper.PostCommentLikeMapper;
import com.finding.post.mapper.PostCommentMapper;
import com.finding.post.mapper.PostDraftMapper;
import com.finding.post.mapper.PostFavoriteMapper;
import com.finding.post.mapper.PostLikeMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.post.vo.PostDraftVO;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock private PostDraftMapper draftMapper;
    @Mock private PostLikeMapper likeMapper;
    @Mock private PostFavoriteMapper favoriteMapper;
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
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PostDraft.class);
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

    // ── 热门「值得推荐」:综合热度排序,不再依赖 is_hot 标记 ──

    @Test
    void listPosts_recommended_usesHeatScore_noIsHotFilter() {
        PostQueryDTO query = new PostQueryDTO();
        query.setTab("hot");
        query.setSortBy("recommended");
        query.setPage(1);
        query.setSize(10);
        when(postMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 10));

        service.listPosts(query, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Post>> cap = ArgumentCaptor.forClass(Wrapper.class);
        verify(postMapper).selectPage(any(), cap.capture());
        String sql = cap.getValue().getTargetSql();
        assertTrue(sql.contains("like_count * 0.6 + view_count * 0.3 + comment_count * 0.1"),
                "值得推荐应按综合热度表达式排序");
        assertTrue(sql.contains("created_at DESC"), "同热度时按发布时间兜底");
        assertFalse(sql.contains("is_hot"), "值得推荐不应再依赖 is_hot 标记");
    }

    @Test
    void listPosts_recommended_allPostsIncluded() {
        PostQueryDTO query = new PostQueryDTO();
        query.setTab("hot");
        query.setSortBy("recommended");
        query.setPage(1);
        query.setSize(10);
        when(postMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 10));

        service.listPosts(query, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Post>> cap = ArgumentCaptor.forClass(Wrapper.class);
        verify(postMapper).selectPage(any(), cap.capture());
        String sql = cap.getValue().getTargetSql();
        // 仅保留基础可见性过滤(status=1 已发布 + review_status=0 审核通过),无 is_hot 条件
        assertTrue(sql.contains("status ="), "仍保留已发布过滤");
        assertTrue(sql.contains("review_status ="), "仍保留审核通过过滤");
    }

    // ── 分类 / 标签 / 置顶排序 ──

    @Test
    void createPost_invalidCategory_rejected() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        PostCreateDTO dto = dto("内容");
        dto.setCategory("nope");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createPost(1L, dto));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createPost_validCategoryAndTags_storedAndDeduped() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        when(userService.getUserProfile(any(), any())).thenReturn(new com.finding.user.vo.UserVO());
        PostCreateDTO dto = dto("内容");
        dto.setCategory("study");
        dto.setTags(List.of("高数", "高数", "考试"));

        service.createPost(1L, dto);

        ArgumentCaptor<Post> cap = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(cap.capture());
        assertEquals("study", cap.getValue().getCategory());
        assertEquals("高数,考试", cap.getValue().getTags());
    }

    @Test
    void createPost_tooManyTags_rejected() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        PostCreateDTO dto = dto("内容");
        dto.setTags(List.of("a", "b", "c", "d", "e", "f"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createPost(1L, dto));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createPost_tagTooLong_rejected() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        PostCreateDTO dto = dto("内容");
        dto.setTags(List.of("这是一个超过十五个字的超长标签内容测试"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createPost(1L, dto));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void listPosts_categoryAndTag_filterApplied() {
        PostQueryDTO query = new PostQueryDTO();
        query.setTab("latest");
        query.setCategory("study");
        query.setTag("高数");
        query.setPage(1);
        query.setSize(10);
        when(postMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 10));

        service.listPosts(query, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Post>> cap = ArgumentCaptor.forClass(Wrapper.class);
        verify(postMapper).selectPage(any(), cap.capture());
        String sql = cap.getValue().getTargetSql();
        assertTrue(sql.contains("category ="), "应含分类过滤");
        assertTrue(sql.contains("tags"), "应含标签过滤");
    }

    @Test
    void listPosts_latest_topFirst() {
        PostQueryDTO query = new PostQueryDTO();
        query.setTab("latest");
        query.setPage(1);
        query.setSize(10);
        when(postMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 10));

        service.listPosts(query, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Post>> cap = ArgumentCaptor.forClass(Wrapper.class);
        verify(postMapper).selectPage(any(), cap.capture());
        String sql = cap.getValue().getTargetSql();
        assertTrue(sql.contains("is_top"), "最新列表应置顶优先排序");
    }

    // ── 收藏 / 可见性 / @提及 ──

    @Test
    void createPost_visibilityDefaultsToPublic() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        when(userService.getUserProfile(any(), any())).thenReturn(new com.finding.user.vo.UserVO());

        service.createPost(1L, dto("内容"));

        ArgumentCaptor<Post> cap = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(cap.capture());
        assertEquals(0, cap.getValue().getVisibility());
    }

    @Test
    void createPost_invalidVisibility_normalizedToPublic() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        when(userService.getUserProfile(any(), any())).thenReturn(new com.finding.user.vo.UserVO());
        PostCreateDTO dto = dto("内容");
        dto.setVisibility(9);

        service.createPost(1L, dto);

        ArgumentCaptor<Post> cap = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(cap.capture());
        assertEquals(0, cap.getValue().getVisibility());
    }

    @Test
    void createPost_mention_notifiesUniqueNickname() {
        when(sensitiveWordFilter.classifyReview(any(String[].class)))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        when(userService.getUserProfile(any(), any())).thenReturn(new com.finding.user.vo.UserVO());
        when(likeMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.selectCount(any())).thenReturn(0L);
        User mentioned = new User();
        mentioned.setId(9L);
        mentioned.setNickname("小王");
        when(userMapper.selectList(any())).thenReturn(List.of(mentioned));

        service.createPost(1L, dto("你好 @小王 在吗"));

        verify(messageService).notify(eq(1L), eq(9L), eq("mention"), any(), any());
    }

    @Test
    void toggleFavorite_insertsWhenAbsent() {
        Post post = post(1L, 2L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(favoriteMapper.selectOne(any())).thenReturn(null);

        service.toggleFavorite(5L, 1L);

        verify(favoriteMapper).insert(any(PostFavorite.class));
    }

    @Test
    void toggleFavorite_deletesWhenPresent() {
        Post post = post(1L, 2L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);
        PostFavorite existing = new PostFavorite();
        existing.setId(7L);
        when(favoriteMapper.selectOne(any())).thenReturn(existing);

        service.toggleFavorite(5L, 1L);

        verify(favoriteMapper).deleteById(7L);
    }

    @Test
    void getPostDetail_privatePost_strangerNotFound() {
        Post post = post(1L, 2L, 0);
        post.setVisibility(2);
        when(postMapper.selectById(1L)).thenReturn(post);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPostDetail(1L, 9L));
        assertEquals(ResultCode.POST_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getPostDetail_friendsOnlyPost_strangerNotFound() {
        Post post = post(1L, 2L, 0);
        post.setVisibility(1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(followMapper.selectCount(any())).thenReturn(0L); // 非好友

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPostDetail(1L, 9L));
        assertEquals(ResultCode.POST_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void listPosts_guest_onlyPublicVisibility() {
        PostQueryDTO query = new PostQueryDTO();
        query.setTab("latest");
        query.setPage(1);
        query.setSize(10);
        when(postMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 10));

        service.listPosts(query, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Post>> cap = ArgumentCaptor.forClass(Wrapper.class);
        verify(postMapper).selectPage(any(), cap.capture());
        String sql = cap.getValue().getTargetSql();
        assertTrue(sql.contains("visibility"), "游客应仅看到公开动态");
    }

    // ── 草稿 ──

    @Test
    void saveDraft_newUser_inserts() {
        when(draftMapper.selectOne(any())).thenReturn(null);
        when(draftMapper.insert(any())).thenReturn(1);

        PostDraftSaveDTO dto = new PostDraftSaveDTO();
        dto.setContent("草稿内容");
        dto.setVisibility(2);
        service.saveDraft(1L, dto);

        ArgumentCaptor<PostDraft> cap = ArgumentCaptor.forClass(PostDraft.class);
        verify(draftMapper).insert(cap.capture());
        assertEquals("草稿内容", cap.getValue().getContent());
        assertEquals(2, cap.getValue().getVisibility());
    }

    @Test
    void saveDraft_existing_updates() {
        PostDraft existing = new PostDraft();
        existing.setId(9L);
        existing.setUserId(1L);
        when(draftMapper.selectOne(any())).thenReturn(existing);

        PostDraftSaveDTO dto = new PostDraftSaveDTO();
        dto.setContent("更新草稿");
        service.saveDraft(1L, dto);

        verify(draftMapper).updateById(existing);
        verify(draftMapper, never()).insert(any());
        assertEquals("更新草稿", existing.getContent());
    }

    @Test
    void getDraft_none_returnsNull() {
        when(draftMapper.selectOne(any())).thenReturn(null);
        assertEquals(null, service.getDraft(1L));
    }

    @Test
    void getDraft_parsesImagesAndTags() {
        PostDraft draft = new PostDraft();
        draft.setUserId(1L);
        draft.setContent("内容");
        draft.setImages("[\"/api/v1/images/a.jpg\"]");
        draft.setTags("tag1,tag2");
        draft.setCategory("study");
        draft.setVisibility(2);
        when(draftMapper.selectOne(any())).thenReturn(draft);

        PostDraftVO vo = service.getDraft(1L);

        assertEquals("内容", vo.getContent());
        assertEquals(List.of("/api/v1/images/a.jpg"), vo.getImages());
        assertEquals(List.of("tag1", "tag2"), vo.getTags());
        assertEquals("study", vo.getCategory());
        assertEquals(2, vo.getVisibility());
    }

    @Test
    void clearDraft_deletes() {
        service.clearDraft(1L);
        verify(draftMapper).delete(any());
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
