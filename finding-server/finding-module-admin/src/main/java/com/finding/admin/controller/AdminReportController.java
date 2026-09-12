package com.finding.admin.controller;

import com.finding.admin.service.AdminReportService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 投诉管理(业务逻辑见 {@link AdminReportService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/reports")
    public Result<PageVO<Map<String, Object>>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminReportService.listReports(page, size, status));
    }

    /** 处理投诉：status=1 已处理，status=2 驳回；记录处理人/时间/意见并通知投诉人 */
    @PutMapping("/reports/{id}/status")
    public Result<Void> updateReportStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminReportService.updateReportStatus(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }
}
