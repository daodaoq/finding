package com.finding.admin.controller;

import com.finding.admin.service.AdminRecommendService;
import com.finding.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 相亲推荐行为统计(曝光/跳过/申请/通过,匿名;业务逻辑见 {@link AdminRecommendService})。
 */
@RestController
@RequestMapping("/api/v1/admin/recommend")
@RequiredArgsConstructor
public class AdminRecommendController {

    private final AdminRecommendService adminRecommendService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminRecommendService.todayStats());
    }
}
