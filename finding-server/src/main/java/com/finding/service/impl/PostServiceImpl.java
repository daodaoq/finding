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
    private final PostCommentMapper commentMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageVO<PostVO> listPosts(PostQueryDTO query, Long currentUserId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1);

        switch (query.getTab()) {
            case "hot" -> wrapper.orderByDesc(Post::getLikeCount, Post::getCreatedAt);
            case "latest" -> wrapper.orderByDesc(Post::getCreatedAt);
            case "following" -> {
                if (currentUserId == null) {
                    return PageVO.of(List.of(), 0L, query.getPage(), query.getSize());
                }
                // Get followed user IDs
                List<Long> followedIds = List.of(); // simplified: would query follow table
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
        // Increment view count
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
    public PageVO<PostVO> listComments(Long postId, int page, int size) {
        Page<PostComment> pg = new Page<>(page, size);
        Page<PostComment> result = commentMapper.selectPage(pg,
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .isNull(PostComment::getParentId)
                        .orderByDesc(PostComment::getCreatedAt));

        // TODO: load child comments and author info
        return PageVO.of(List.<PostVO>of(), result.getTotal(), page, size);
    }

    @Override
    public PostVO addComment(Long userId, Long postId, Long parentId, String content) {
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
        return toVO(post, userId);
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
