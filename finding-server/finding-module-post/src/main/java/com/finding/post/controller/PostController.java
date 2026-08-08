package com.finding.post.controller;

import com.finding.common.Result;
import com.finding.user.common.VerificationGuard;
import com.finding.post.dto.PostCreateDTO;
import com.finding.post.dto.PostQueryDTO;
import com.finding.user.security.JwtInterceptor;
import com.finding.post.service.PostService;
import com.finding.post.vo.CommentVO;
import com.finding.common.PageVO;
import com.finding.post.vo.PostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final VerificationGuard verificationGuard;

    @GetMapping
    public Result<PageVO<PostVO>> list(@Valid PostQueryDTO query) {
        return Result.ok(postService.listPosts(query, JwtInterceptor.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        return Result.ok(postService.getPostDetail(id, JwtInterceptor.getCurrentUserId()));
    }

    @PostMapping
    public Result<PostVO> create(@Valid @RequestBody PostCreateDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        verificationGuard.checkVerified(userId); // 未认证用户不可发帖
        return Result.ok(postService.createPost(userId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        postService.deletePost(userId, id);
        return Result.ok();
    }

    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        postService.toggleLike(userId, id);
        return Result.ok();
    }

    @GetMapping("/{id}/comments")
    public Result<PageVO<CommentVO>> comments(@PathVariable Long id,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.listComments(id, page, size, JwtInterceptor.getCurrentUserId()));
    }

    @PostMapping("/{id}/comments")
    public Result<CommentVO> addComment(@PathVariable Long id,
                                      @RequestParam(required = false) Long parentId,
                                      @RequestParam String content) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        verificationGuard.checkVerified(userId); // 未认证用户不可评论
        return Result.ok(postService.addComment(userId, id, parentId, content));
    }

    @PostMapping("/{id}/comments/{commentId}/like")
    public Result<Void> likeComment(@PathVariable Long id, @PathVariable Long commentId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        postService.toggleCommentLike(userId, commentId);
        return Result.ok();
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        postService.deleteComment(userId, commentId);
        return Result.ok();
    }

    /** 我发布的动态 */
    @GetMapping("/my")
    public Result<PageVO<PostVO>> myPosts(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        return Result.ok(postService.getMyPosts(userId, page, size));
    }

    /** 我点赞过的动态 */
    @GetMapping("/my-likes")
    public Result<PageVO<PostVO>> myLikes(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        return Result.ok(postService.getMyLikedPosts(userId, page, size));
    }
}
