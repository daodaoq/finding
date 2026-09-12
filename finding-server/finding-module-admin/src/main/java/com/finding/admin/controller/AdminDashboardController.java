package com.finding.admin.controller;

import com.finding.admin.service.AdminDashboardService;
import com.finding.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 数据面板(业务与查询见 {@link AdminDashboardService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(adminDashboardService.summary());
    }

    /** 近 N 天趋势(注册/动态/搭子/活跃用户) */
    @GetMapping("/dashboard/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(adminDashboardService.trend(days));
    }

    /** 质量指标:性别比/认证率/留存率/审核时效 */
    @GetMapping("/dashboard/quality")
    public Result<Map<String, Object>> quality() {
        return Result.ok(adminDashboardService.quality());
    }
}
