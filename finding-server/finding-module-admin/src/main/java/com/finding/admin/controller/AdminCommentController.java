package com.finding.admin.controller;

import com.finding.admin.service.AdminCommentService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员 - 评论管理(业务逻辑见 {@link AdminCommentService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    @GetMapping("/comments")
    public Result<PageVO<Map<String, Object>>> listComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminCommentService.listComments(page, size, keyword));
    }

    /** 编辑评论内容(管理员可改一切) */
    @PutMapping("/comments/{id}")
    public Result<Void> updateComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminCommentService.updateComment(id, body);
        return Result.ok();
    }

    /** 软删除评论(保留审计,前端显示占位) */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        adminCommentService.deleteComment(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }
}
