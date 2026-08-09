package com.finding.user.controller;

import com.finding.common.Result;
import com.finding.user.dto.UserSettingsDTO;
import com.finding.user.entity.UserSettings;
import com.finding.user.security.JwtInterceptor;
import com.finding.user.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户全局设置 —— 聊天通用 / 加好友方式 / 个人权限。
 */
@RestController
@RequestMapping("/api/v1/user-settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    public Result<UserSettings> getSettings() {
        return Result.ok(userSettingsService.getSettings(JwtInterceptor.getCurrentUserId()));
    }

    @PutMapping
    public Result<Void> updateSettings(@RequestBody UserSettingsDTO dto) {
        userSettingsService.updateSettings(JwtInterceptor.getCurrentUserId(), dto);
        return Result.ok();
    }
}
