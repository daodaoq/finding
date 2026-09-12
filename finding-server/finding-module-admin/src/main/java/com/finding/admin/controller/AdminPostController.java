package com.finding.admin.controller;

import com.finding.admin.service.AdminPostService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.post.dto.PostCreateDTO;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 动态管理(业务逻辑见 {@link AdminPostService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;

    @GetMapping("/posts")
    public Result<PageVO<Map<String, Object>>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminPostService.listPosts(page, size, keyword));
    }

    /** 编辑动态(管理端,无所有权限制) */
    @PutMapping("/posts/{id}")
    public Result<Void> updatePost(@PathVariable Long id, @RequestBody PostCreateDTO dto) {
        adminPostService.updatePost(id, dto);
        return Result.ok();
    }

    @PutMapping("/posts/{id}/status")
    public Result<Void> updatePostStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminPostService.updatePostStatus(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }

    /** 设置置顶/精华(isTop/isHot 传 1 或 0;不传的字段保持不变) */
    @PutMapping("/posts/{id}/flag")
    public Result<Void> updatePostFlag(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminPostService.updatePostFlag(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        adminPostService.deletePost(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 待审核队列(review_status=1) */
    @GetMapping("/posts/review")
    public Result<PageVO<Map<String, Object>>> reviewQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminPostService.reviewQueue(page, size));
    }

    /** 审核处理:pass=true 通过(发布),false 拒绝(需 reason);记录审核人/时间/原因,拒绝时通知作者 */
    @PutMapping("/posts/{id}/review")
    public Result<Void> reviewPost(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminPostService.reviewPost(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }

    /** 批量审核处理:ids + pass + reason,逐条应用并记录审计 */
    @PostMapping("/posts/review/batch")
    public Result<Void> reviewBatch(@RequestBody Map<String, Object> body) {
        adminPostService.reviewBatch(JwtInterceptor.getCurrentUserId(), body);
        return Result.ok();
    }
}
