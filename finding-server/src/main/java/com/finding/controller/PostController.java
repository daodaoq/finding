package com.finding.controller;

import com.finding.common.Result;
import com.finding.dto.PostCreateDTO;
import com.finding.dto.PostQueryDTO;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.PostService;
import com.finding.vo.PageVO;
import com.finding.vo.PostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

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
    public Result<PageVO<PostVO>> comments(@PathVariable Long id,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.listComments(id, page, size));
    }

    @PostMapping("/{id}/comments")
    public Result<PostVO> addComment(@PathVariable Long id,
                                      @RequestParam(required = false) Long parentId,
                                      @RequestParam String content) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        return Result.ok(postService.addComment(userId, id, parentId, content));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        postService.deleteComment(userId, commentId);
        return Result.ok();
    }
}
