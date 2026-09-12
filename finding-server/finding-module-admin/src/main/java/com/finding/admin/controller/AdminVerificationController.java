package com.finding.admin.controller;

import com.finding.admin.service.AdminVerificationService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 学生认证审核(业务逻辑见 {@link AdminVerificationService})。
 * 审核结果通过事件通知被审核用户(见 app 模块 UserVerifiedListener)。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    @GetMapping("/verifications")
    public Result<PageVO<Map<String, Object>>> listVerifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminVerificationService.listVerifications(page, size, status));
    }

    /** 通过认证：审核人取当前登录管理员(不再接受 reviewerId 查询参数,避免审核人可被伪造) */
    @PutMapping("/verifications/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        adminVerificationService.approve(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 拒绝认证：审核人取当前登录管理员 */
    @PutMapping("/verifications/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam(defaultValue = "") String comment) {
        adminVerificationService.reject(JwtInterceptor.getCurrentUserId(), id, comment);
        return Result.ok();
    }
}
