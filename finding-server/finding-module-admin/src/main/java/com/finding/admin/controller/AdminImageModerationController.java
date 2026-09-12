package com.finding.admin.controller;

import com.finding.admin.service.AdminImageModerationService;
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
 * 管理员 - 图片审核复核队列(业务逻辑见 {@link AdminImageModerationService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminImageModerationController {

    private final AdminImageModerationService adminImageModerationService;

    /** 待复核队列(verdict=2 送审 且 status=0 待复核) */
    @GetMapping("/image-moderation")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminImageModerationService.reviewQueue(page, size));
    }

    /** 复核:pass=true 放行,false 删除图片并通知上传者 */
    @PutMapping("/image-moderation/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminImageModerationService.handle(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }
}
