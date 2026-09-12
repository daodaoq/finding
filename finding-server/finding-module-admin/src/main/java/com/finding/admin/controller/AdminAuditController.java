package com.finding.admin.controller;

import com.finding.admin.service.AdminAuditService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 操作审计日志查看(业务逻辑见 {@link AdminAuditService})。
 * 敏感操作(封禁/举报处理/内容审核等)写入 operation_log,此处提供分页查询与筛选。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping("/audit-logs")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminAuditService.listLogs(page, size, action, targetType, operatorId, keyword));
    }
}
