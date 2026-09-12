package com.finding.admin.controller;

import com.finding.admin.service.AdminUserService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;
import com.finding.user.security.JwtInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 管理员 - 用户管理(业务逻辑见 {@link AdminUserService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    public Result<PageVO<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminUserService.listUsers(page, size, keyword));
    }

    /** 新建用户 */
    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        return Result.ok(adminUserService.createUser(body));
    }

    /** 获取用户详情（含完整字段） */
    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        return Result.ok(adminUserService.getUserDetail(id));
    }

    /** 编辑用户信息 */
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminUserService.updateUser(id, body);
        return Result.ok();
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminUserService.toggleUserStatus(id, body);
        return Result.ok();
    }

    /** 封禁用户(支持按天 + 原因):days>0 封禁 days 天,days=0 永久;发布事件让在线用户实时收到提示 */
    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminUserService.banUser(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }

    /** 警告用户一次:记录 + 站内通知 */
    @PutMapping("/users/{id}/warn")
    public Result<Void> warnUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminUserService.warnUser(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }

    /** 查看任意用户的情感简历(绕过互换权限，仅管理员可见) */
    @GetMapping("/users/{id}/resume")
    public Result<UserResume> getUserResume(@PathVariable Long id) {
        return Result.ok(adminUserService.getUserResume(id));
    }

    /** 编辑任意用户的情感简历(管理员可改一切) */
    @PutMapping("/users/{id}/resume")
    public Result<Void> updateUserResume(@PathVariable Long id, @Valid @RequestBody UserResumeDTO dto) {
        adminUserService.updateUserResume(id, dto);
        return Result.ok();
    }
}
