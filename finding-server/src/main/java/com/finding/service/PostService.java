package com.finding.service;

import com.finding.dto.PostCreateDTO;
import com.finding.dto.PostQueryDTO;
import com.finding.vo.CommentVO;
import com.finding.vo.PageVO;
import com.finding.vo.PostVO;

public interface PostService {

    PageVO<PostVO> listPosts(PostQueryDTO query, Long currentUserId);
    PostVO getPostDetail(Long postId, Long currentUserId);
    PostVO createPost(Long userId, PostCreateDTO dto);
    void deletePost(Long userId, Long postId);
    void toggleLike(Long userId, Long postId);
    PageVO<CommentVO> listComments(Long postId, int page, int size, Long currentUserId);
    CommentVO addComment(Long userId, Long postId, Long parentId, String content);
    void deleteComment(Long userId, Long commentId);

    /** 获取当前用户发布的动态列表 */
    PageVO<PostVO> getMyPosts(Long userId, int page, int size);

    /** 获取当前用户点赞过的动态列表 */
    PageVO<PostVO> getMyLikedPosts(Long userId, int page, int size);
}
