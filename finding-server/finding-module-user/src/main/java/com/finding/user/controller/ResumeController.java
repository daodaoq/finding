package com.finding.user.controller;

import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;
import com.finding.user.security.JwtInterceptor;
import com.finding.user.service.UserResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final UserResumeService userResumeService;

    /** 获取我的情感简历(未填写返回 data=null) */
    @GetMapping("/me")
    public Result<UserResume> getMyResume() {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(userResumeService.getMyResume(userId));
    }

    /** 保存/更新我的情感简历 */
    @PutMapping("/me")
    public Result<Void> saveMyResume(@Valid @RequestBody UserResumeDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        userResumeService.saveResume(userId, dto);
        return Result.ok();
    }
}
