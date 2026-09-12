package com.finding.admin.controller;

import com.finding.admin.service.AdminFeedbackService;
import com.finding.common.PageVO;
import com.finding.common.Result;
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
 * 管理员 - 用户反馈/客服工单处理(业务逻辑见 {@link AdminFeedbackService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final AdminFeedbackService adminFeedbackService;

    @GetMapping("/feedbacks")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminFeedbackService.listFeedbacks(page, size, status));
    }

    /** 标记工单为已处理(1)/重新打开(0) */
    @PutMapping("/feedbacks/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminFeedbackService.updateStatus(id, body);
        return Result.ok();
    }
}
