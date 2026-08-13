package com.finding.post.controller;

import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.post.service.AppealService;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 内容申诉 —— 用户对被拒/下架内容发起申诉与查询 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;

    /** 对审核未通过或被下架的动态发起申诉(同一内容有次数上限) */
    @PostMapping("/posts/{id}/appeal")
    public Result<Void> appeal(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        appealService.appeal(userId, id, reason);
        return Result.ok();
    }

    /** 我的申诉记录 */
    @GetMapping("/appeals/mine")
    public Result<List<Map<String, Object>>> myAppeals() {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(appealService.myAppeals(userId));
    }
}
