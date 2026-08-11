package com.finding.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.post.dto.PostCreateDTO;
import com.finding.post.dto.PostQueryDTO;


import com.finding.post.service.PostService;
import com.finding.user.service.UserService;
import com.finding.user.service.UserWriteGuard;
import com.finding.post.vo.CommentVO;
import com.finding.common.PageVO;
import com.finding.common.util.XssUtil;
import com.finding.common.word.ReviewResult;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.post.vo.PostVO;
import com.finding.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.finding.post.entity.Post;
import com.finding.post.entity.PostComment;
import com.finding.post.entity.PostCommentLike;
import com.finding.post.entity.PostLike;
import com.finding.user.entity.User;
import com.finding.user.entity.UserFollow;
import com.finding.post.mapper.PostCommentLikeMapper;
import com.finding.post.mapper.PostCommentMapper;
import com.finding.post.mapper.PostLikeMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.message.service.MessageService;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostLikeMapper likeMapper;
    private final PostCommentLikeMapper commentLikeMapper;
    private final PostCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;
    private final UserService userService;
    private final MessageService messageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final UserWriteGuard userWriteGuard;

    @Override
    public PageVO<PostVO> listPosts(PostQueryDTO query, Long currentUserId) {
        // 只返回已发布(审核通过)的动态
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getReviewStatus, 0);

        switch (query.getTab()) {
            case "hot" -> {
                // 热门子排序: views(浏览量最高), likes(点赞率最高), recommended(值得推荐)
                String sortBy = query.getSortBy();
                if ("views".equals(sortBy)) {
                    wrapper.orderByDesc(Post::getViewCount);
                } else if ("likes".equals(sortBy)) {
                    wrapper.orderByDesc(Post::getLikeCount);
                } else {
                    // recommended: 综合热度 = 点赞 * 0.6 + 浏览量 * 0.3 + 评论 * 0.1
                    wrapper.eq(Post::getIsHot, 1)
                           .orderByDesc(Post::getLikeCount);
                }
                wrapper.orderByDesc(Post::getCreatedAt);
            }
            case "latest" -> wrapper.orderByDesc(Post::getCreatedAt);
            case "following" -> {
                if (currentUserId == null) {
                    return PageVO.of(List.of(), 0L, query.getPage(), query.getSize());
                }
                // 查询关注的用户ID
                List<Long> followedIds = followMapper.selectList(
                        new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, currentUserId))
                        .stream().map(UserFollow::getFolloweeId).collect(Collectors.toList());
                if (followedIds.isEmpty()) {
                    return PageVO.of(List.of(), 0L, query.getPage(), query.getSize());
                }
                wrapper.in(Post::getUserId, followedIds);
                wrapper.orderByDesc(Post::getCreatedAt);
            }
            default -> wrapper.orderByDesc(Post::getCreatedAt);
        }

        Page<Post> page = new Page<>(query.getPage(), query.getSize());
        Page<Post> result = postMapper.selectPage(page, wrapper);

        // 同步评论数(仅正常评论)
        result.getRecords().forEach(p -> {
            Long c = commentMapper.selectCount(
                    new LambdaQueryWrapper<PostComment>()
                            .eq(PostComment::getPostId, p.getId())
                            .eq(PostComment::getStatus, 0));
            if (!c.equals((long) p.getCommentCount())) {
                p.setCommentCount(c.intValue());
                postMapper.updateById(p);
            }
        });

        List<PostVO> records = result.getRecords().stream()
                .map(p -> toVO(p, currentUserId))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public PostVO getPostDetail(Long postId, Long currentUserId) {
        Post post = assertPostVisible(postId, currentUserId);
        // 自动同步实际评论数
        Long realCount = commentMapper.selectCount(
                new LambdaQueryWrapper<PostComment>().eq(PostComment::getPostId, postId));
        post.setCommentCount(realCount.intValue());
        // 增加浏览量
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
        return toVO(post, currentUserId);
    }

    /**
     * 动态可见性校验:存在 / 未删除 / 审核通过(待审或拒绝仅作者可见,他人视为不存在)。
     * 评论列表、评论、点赞等接口统一复用,防止通过评论接口读取不可见动态的数据。
     */
    private Post assertPostVisible(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        Integer rs = post.getReviewStatus() != null ? post.getReviewStatus() : 0;
        if (rs != 0 && (currentUserId == null || !post.getUserId().equals(currentUserId))) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        return post;
    }

    @Override
    @Transactional
    public PostVO createPost(Long userId, PostCreateDTO dto) {
        userWriteGuard.checkWritable(userId);
        String content = XssUtil.clean(dto.getContent());
        // 拦截词 → 拒绝发布;送审词 → 进入审核队列;干净 → 直接发布
        ReviewResult review = sensitiveWordFilter.classifyReview(content);
        throwIfBlocked(review);
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setImages(dto.getImages() != null ? String.join(",", dto.getImages()) : null);
        post.setLocation(dto.getLocation());
        post.setCity(dto.getCity());
        post.setLatitude(dto.getLatitude());
        post.setLongitude(dto.getLongitude());
        post.setStatus(1);
        post.setReviewStatus(review.hasReview() ? 1 : 0);
        postMapper.insert(post);
        return toVO(post, userId);
    }

    @Override
    public PostVO updatePost(Long userId, Long postId, PostCreateDTO dto) {
        userWriteGuard.checkWritable(userId); // 禁言/冻结用户不可编辑动态
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能编辑自己的动态");
        }
        post.setContent(XssUtil.clean(dto.getContent()));
        post.setImages(dto.getImages() != null ? String.join(",", dto.getImages()) : null);
        post.setLocation(dto.getLocation());
        post.setCity(dto.getCity());
        if (dto.getLatitude() != null) post.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) post.setLongitude(dto.getLongitude());
        // 编辑后重新审核:拦截词拒绝;送审词回到待审;干净则发布并清除拒绝原因
        ReviewResult review = sensitiveWordFilter.classifyReview(post.getContent());
        throwIfBlocked(review);
        post.setReviewStatus(review.hasReview() ? 1 : 0);
        post.setReviewReason(null);
        postMapper.updateById(post);
        return toVO(post, userId);
    }

    @Override
    public void deletePost(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能删除自己的动态");
        }
        post.setStatus(0);
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void toggleLike(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }

        PostLike existing = likeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));

        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postMapper.updateById(post);
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);

            // Create notification if not self-like
            if (!post.getUserId().equals(userId)) {
                messageService.notify(userId, post.getUserId(), "like", "赞了你的动态", postId);
            }
        }
    }

    @Override
    public PageVO<CommentVO> listComments(Long postId, int page, int size, Long currentUserId) {
        // 评论可见性:动态不可见(不存在/删除/待审拒审非作者)时不返回评论
        assertPostVisible(postId, currentUserId);
        // 查询一级评论（parent_id IS NULL）
        Page<PostComment> pg = new Page<>(page, size);
        Page<PostComment> result = commentMapper.selectPage(pg,
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .isNull(PostComment::getParentId)
                        .orderByDesc(PostComment::getCreatedAt));

        List<CommentVO> records = result.getRecords().stream()
                .map(c -> toCommentVO(c, currentUserId))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public CommentVO addComment(Long userId, Long postId, Long parentId, String content) {
        userWriteGuard.checkWritable(userId);
        Post post = assertPostVisible(postId, userId);
        // 父评论归属校验:必须属于当前动态、未删除
        PostComment parent = null;
        if (parentId != null) {
            parent = commentMapper.selectOne(new LambdaQueryWrapper<PostComment>()
                    .eq(PostComment::getId, parentId)
                    .eq(PostComment::getPostId, postId));
            if (parent == null || (parent.getStatus() != null && parent.getStatus() == 1)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "父评论不存在或已删除");
            }
        }
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(XssUtil.clean(content));
        sensitiveWordFilter.assertClean(content);
        commentMapper.insert(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        // 回复了别人的评论 → 通知评论作者;否则评论了动态 → 通知帖主(均非自己)
        if (parent != null) {
            if (!parent.getUserId().equals(userId)) {
                messageService.notify(userId, parent.getUserId(), "comment", "回复了你的评论", postId);
            }
        } else if (!post.getUserId().equals(userId)) {
            messageService.notify(userId, post.getUserId(), "comment", "评论了你的动态", postId);
        }

        return toCommentVO(comment, userId);
    }

    /** 评论转 VO，含作者信息 + 前3条子回复；已删除评论显示占位文案 */
    private CommentVO toCommentVO(PostComment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        boolean deleted = comment.getStatus() != null && comment.getStatus() == 1;
        vo.setStatus(deleted ? 1 : 0);
        vo.setContent(deleted ? "该评论已删除" : comment.getContent());
        vo.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        vo.setCreatedAt(comment.getCreatedAt());

        // 作者信息
        User author = userMapper.selectById(comment.getUserId());
        if (author != null) {
            vo.setNickname(author.getNickname());
            vo.setAvatar(author.getAvatar());
        }

        // 评论点赞状态(已删除不展示)
        if (currentUserId != null && !deleted) {
            vo.setIsLiked(commentLikeMapper.selectCount(new LambdaQueryWrapper<PostCommentLike>()
                    .eq(PostCommentLike::getCommentId, comment.getId())
                    .eq(PostCommentLike::getUserId, currentUserId)) > 0);
        }

        // 加载子回复（最多3条；父评论已删除仍展示其下的子回复）
        List<PostComment> children = commentMapper.selectList(
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getParentId, comment.getId())
                        .orderByAsc(PostComment::getCreatedAt)
                        .last("LIMIT 3"));
        if (!children.isEmpty()) {
            vo.setReplies(children.stream()
                    .map(c -> toCommentVO(c, currentUserId))
                    .collect(Collectors.toList()));
            Long total = commentMapper.selectCount(
                    new LambdaQueryWrapper<PostComment>()
                            .eq(PostComment::getParentId, comment.getId())
                            .eq(PostComment::getStatus, 0));
            vo.setReplyCount(total.intValue());
        }

        return vo;
    }

    @Override
    public void deleteComment(Long userId, Long postId, Long commentId) {
        // 限定评论归属动态,防止跨动态 commentId 操作他人动态资源
        PostComment comment = commentMapper.selectOne(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getId, commentId)
                .eq(PostComment::getPostId, postId));
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能删除自己的评论");
        }
        // 软删除:保留记录与审计,前端展示"该评论已删除"占位
        comment.setStatus(1);
        commentMapper.updateById(comment);
    }

    @Override
    @Transactional
    public void toggleCommentLike(Long userId, Long postId, Long commentId) {
        // 动态不可见时不可点赞评论
        assertPostVisible(postId, userId);
        PostComment comment = commentMapper.selectOne(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getId, commentId)
                .eq(PostComment::getPostId, postId));
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }

        PostCommentLike existing = commentLikeMapper.selectOne(new LambdaQueryWrapper<PostCommentLike>()
                .eq(PostCommentLike::getCommentId, commentId)
                .eq(PostCommentLike::getUserId, userId));

        if (existing != null) {
            // 取消点赞
            commentLikeMapper.deleteById(existing.getId());
            comment.setLikeCount(Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 0) - 1));
            commentMapper.updateById(comment);
        } else {
            // 点赞
            PostCommentLike like = new PostCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeMapper.insert(like);
            comment.setLikeCount((comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1);
            commentMapper.updateById(comment);

            // 通知评论作者（非自己）
            if (!comment.getUserId().equals(userId)) {
                messageService.notify(userId, comment.getUserId(), "like", "赞了你的评论", comment.getPostId());
            }
        }
    }

    @Override
    public PageVO<PostVO> getMyPosts(Long userId, int page, int size) {
        Page<Post> pg = new Page<>(page, size);
        Page<Post> result = postMapper.selectPage(pg,
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .orderByDesc(Post::getCreatedAt));
        List<PostVO> records = result.getRecords().stream()
                .map(p -> toVO(p, userId))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public PageVO<PostVO> getMyLikedPosts(Long userId, int page, int size) {
        Page<PostLike> likePage = new Page<>(page, size);
        Page<PostLike> likes = likeMapper.selectPage(likePage,
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .orderByDesc(PostLike::getCreatedAt));

        List<Long> postIds = likes.getRecords().stream()
                .map(PostLike::getPostId).collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, page, size);
        }

        List<Post> posts = postMapper.selectBatchIds(postIds);
        List<PostVO> records = posts.stream()
                .map(p -> toVO(p, userId))
                .collect(Collectors.toList());
        return PageVO.of(records, likes.getTotal(), page, size);
    }

    @Override
    public PageVO<PostVO> getUserPublicPosts(Long userId, Long viewerId, int page, int size) {
        Page<Post> pg = new Page<>(page, size);
        Page<Post> result = postMapper.selectPage(pg,
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getStatus, 1)
                        .eq(Post::getReviewStatus, 0)
                        .orderByDesc(Post::getCreatedAt));
        List<PostVO> records = result.getRecords().stream()
                .map(p -> toVO(p, viewerId))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    /** 命中「拦截」动作的违禁词 → 拒绝发布(提示具体词) */
    private void throwIfBlocked(ReviewResult review) {
        if (review.hasBlocking()) {
            String joined = review.blocking().stream().map(w -> "「" + w + "」").collect(Collectors.joining());
            throw new BusinessException(ResultCode.CONTENT_BLOCKED, "内容包含违禁词:" + joined);
        }
    }

    private PostVO toVO(Post post, Long currentUserId) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setContent(post.getContent());
        vo.setImages(post.getImages() != null ? List.of(post.getImages().split(",")) : List.of());
        vo.setLocation(post.getLocation());
        vo.setCity(post.getCity());
        vo.setLatitude(post.getLatitude());
        vo.setLongitude(post.getLongitude());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setShareCount(post.getShareCount());
        vo.setIsHot(post.getIsHot());
        vo.setIsTop(post.getIsTop());
        vo.setReviewStatus(post.getReviewStatus() != null ? post.getReviewStatus() : 0);
        vo.setReviewReason(post.getReviewReason());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setUpdatedAt(post.getUpdatedAt());

        // Author
        if (post.getUserId() != null) {
            vo.setAuthor(userService.getUserProfile(post.getUserId(), currentUserId));
        }
        // Like status
        if (currentUserId != null) {
            vo.setIsLiked(likeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                    .eq(PostLike::getPostId, post.getId())
                    .eq(PostLike::getUserId, currentUserId)) > 0);
        }
        return vo;
    }
}
