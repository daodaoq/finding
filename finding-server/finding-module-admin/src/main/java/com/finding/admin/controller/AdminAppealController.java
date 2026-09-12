package com.finding.admin.controller;

import com.finding.admin.service.AdminAppealService;
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
 * 管理员 - 申诉处理(业务逻辑见 {@link AdminAppealService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAppealController {

    private final AdminAppealService adminAppealService;

    @GetMapping("/appeals")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminAppealService.listAppeals(page, size, status));
    }

    /** 处理申诉:pass=true 通过(重新发布),false 驳回(保留原结果) */
    @PutMapping("/appeals/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminAppealService.handle(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }
}
