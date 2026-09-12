package com.finding.admin.controller;

import com.finding.admin.service.AdminLoveGuideService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.post.entity.LoveGuide;
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
 * 管理员 - 恋爱经验投稿审核(业务逻辑见 {@link AdminLoveGuideService})。
 */
@RestController
@RequestMapping("/api/v1/admin/love-guides")
@RequiredArgsConstructor
public class AdminLoveGuideController {

    private final AdminLoveGuideService adminLoveGuideService;

    /** 投稿列表:展示全部投稿(待审核/已通过/已拒绝) */
    @GetMapping("/review")
    public Result<PageVO<LoveGuide>> queue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminLoveGuideService.queue(page, size));
    }

    /** 审核投稿:pass=true 通过,false 拒绝(通知作者) */
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminLoveGuideService.review(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }
}
