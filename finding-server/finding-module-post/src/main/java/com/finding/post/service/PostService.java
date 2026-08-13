package com.finding.post.service;

import com.finding.post.dto.PostCreateDTO;
import com.finding.post.dto.PostQueryDTO;
import com.finding.post.vo.CommentVO;
import com.finding.common.PageVO;
import com.finding.post.vo.PostVO;

public interface PostService {

    PageVO<PostVO> listPosts(PostQueryDTO query, Long currentUserId);
    PostVO getPostDetail(Long postId, Long currentUserId);
    PostVO createPost(Long userId, PostCreateDTO dto);
    PostVO updatePost(Long userId, Long postId, PostCreateDTO dto);
    void deletePost(Long userId, Long postId);
    void toggleLike(Long userId, Long postId);
    PageVO<CommentVO> listComments(Long postId, int page, int size, Long currentUserId);
    CommentVO addComment(Long userId, Long postId, Long parentId, String content);
    void deleteComment(Long userId, Long postId, Long commentId);
    void toggleCommentLike(Long userId, Long postId, Long commentId);

    /** 获取当前用户发布的动态列表 */
    PageVO<PostVO> getMyPosts(Long userId, int page, int size);

    /** 获取当前用户点赞过的动态列表 */
    PageVO<PostVO> getMyLikedPosts(Long userId, int page, int size);

    /** 收藏/取消收藏(幂等,唯一约束兜底) */
    void toggleFavorite(Long userId, Long postId);

    /** 获取当前用户收藏的动态列表 */
    PageVO<PostVO> getMyFavorites(Long userId, int page, int size);

    /** 获取指定用户的公开动态(仅展示中,他人主页用) */
    PageVO<PostVO> getUserPublicPosts(Long userId, Long viewerId, int page, int size);
}
