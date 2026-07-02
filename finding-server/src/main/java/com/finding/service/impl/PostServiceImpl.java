package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.dto.PostCreateDTO;
import com.finding.dto.PostQueryDTO;
import com.finding.entity.*;
import com.finding.mapper.*;
import com.finding.service.PostService;
import com.finding.service.UserService;
import com.finding.vo.CommentVO;
import com.finding.vo.PageVO;
import com.finding.vo.PostVO;
import com.finding.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostLikeMapper likeMapper;
    private final PostCommentLikeMapper commentLikeMapper;
    private final PostCommentMapper commentMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageVO<PostVO> listPosts(PostQueryDTO query, Long currentUserId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1);

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

        // 同步评论数
        result.getRecords().forEach(p -> {
            Long c = commentMapper.selectCount(
                    new LambdaQueryWrapper<PostComment>().eq(PostComment::getPostId, p.getId()));
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
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        // 自动同步实际评论数
        Long realCount = commentMapper.selectCount(
                new LambdaQueryWrapper<PostComment>().eq(PostComment::getPostId, postId));
        post.setCommentCount(realCount.intValue());
        // 增加浏览量
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
        return toVO(post, currentUserId);
    }

    @Override
    @Transactional
    public PostVO createPost(Long userId, PostCreateDTO dto) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(dto.getContent());
        post.setImages(dto.getImages() != null ? String.join(",", dto.getImages()) : null);
        post.setLocation(dto.getLocation());
        post.setCity(dto.getCity());
        post.setLatitude(dto.getLatitude());
        post.setLongitude(dto.getLongitude());
        post.setStatus(1);
        postMapper.insert(post);
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
                Message msg = new Message();
                msg.setFromUserId(userId);
                msg.setToUserId(post.getUserId());
                msg.setType("like");
                msg.setContent("赞了你的动态");
                msg.setRelatedId(postId);
                messageMapper.insert(msg);
            }
        }
    }

    @Override
    public PageVO<CommentVO> listComments(Long postId, int page, int size, Long currentUserId) {
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
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        commentMapper.insert(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        // 如果回复了别人的评论，发通知
        if (parentId != null) {
            PostComment parent = commentMapper.selectById(parentId);
            if (parent != null && !parent.getUserId().equals(userId)) {
                Message msg = new Message();
                msg.setFromUserId(userId);
                msg.setToUserId(parent.getUserId());
                msg.setType("comment");
                msg.setContent("回复了你的评论");
                msg.setRelatedId(postId);
                messageMapper.insert(msg);
            }
        } else {
            // 评论了帖子，通知帖主
            if (!post.getUserId().equals(userId)) {
                Message msg = new Message();
                msg.setFromUserId(userId);
                msg.setToUserId(post.getUserId());
                msg.setType("comment");
                msg.setContent("评论了你的动态");
                msg.setRelatedId(postId);
                messageMapper.insert(msg);
            }
        }

        return toCommentVO(comment, userId);
    }

    /** 评论转 VO，含作者信息 + 前3条子回复 */
    private CommentVO toCommentVO(PostComment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        vo.setCreatedAt(comment.getCreatedAt());

        // 作者信息
        User author = userMapper.selectById(comment.getUserId());
        if (author != null) {
            vo.setNickname(author.getNickname());
            vo.setAvatar(author.getAvatar());
        }

        // 评论点赞状态
        if (currentUserId != null) {
            vo.setIsLiked(commentLikeMapper.selectCount(new LambdaQueryWrapper<PostCommentLike>()
                    .eq(PostCommentLike::getCommentId, comment.getId())
                    .eq(PostCommentLike::getUserId, currentUserId)) > 0);
        }

        // 加载子回复（最多3条）
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
                            .eq(PostComment::getParentId, comment.getId()));
            vo.setReplyCount(total.intValue());
        }

        return vo;
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
    }

    @Override
    @Transactional
    public void toggleCommentLike(Long userId, Long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
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
                Message msg = new Message();
                msg.setFromUserId(userId);
                msg.setToUserId(comment.getUserId());
                msg.setType("like");
                msg.setContent("赞了你的评论");
                msg.setRelatedId(comment.getPostId());
                messageMapper.insert(msg);
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
