package com.finding.admin.controller;

import com.finding.admin.service.AdminMateService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.mate.dto.AdminMateReviewDTO;
import com.finding.mate.dto.AdminMateStatusDTO;
import com.finding.mate.dto.AdminMateUpdateDTO;
import com.finding.user.security.JwtInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 搭子邀约管理(业务逻辑见 {@link AdminMateService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMateController {

    private final AdminMateService adminMateService;

    @GetMapping("/mates")
    public Result<PageVO<Map<String, Object>>> listMates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminMateService.listMates(page, size, keyword, status));
    }

    /** 管理员编辑邀约:强类型校验，禁止绕过人数、时间与内容约束。 */
    @PutMapping("/mates/{id}")
    public Result<Void> updateMate(@PathVariable Long id, @Valid @RequestBody AdminMateUpdateDTO body) {
        adminMateService.updateMate(id, body);
        return Result.ok();
    }

    /** 下架邀约：status=0(软删，列表页自动隐藏) */
    @PutMapping("/mates/{id}/status")
    public Result<Void> updateMateStatus(@PathVariable Long id, @Valid @RequestBody AdminMateStatusDTO body) {
        adminMateService.updateMateStatus(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }

    @PutMapping("/mates/{id}/close")
    public Result<Void> closeMate(@PathVariable Long id) {
        adminMateService.closeMate(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    @PutMapping("/mates/{id}/cancel")
    public Result<Void> cancelMate(@PathVariable Long id) {
        adminMateService.cancelMate(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 搭子待审核队列(review_status=1) */
    @GetMapping("/mates/review")
    public Result<PageVO<Map<String, Object>>> reviewQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminMateService.reviewQueue(page, size));
    }

    /** 搭子审核处理:pass=true 通过发布,false 拒绝(需 reason,通知作者) */
    @PutMapping("/mates/{id}/review")
    public Result<Void> reviewMate(@PathVariable Long id, @Valid @RequestBody AdminMateReviewDTO body) {
        adminMateService.reviewMate(JwtInterceptor.getCurrentUserId(), id, body);
        return Result.ok();
    }

    /** 软删除邀约，保留参与、举报和审核审计记录。 */
    @DeleteMapping("/mates/{id}")
    public Result<Void> deleteMate(@PathVariable Long id) {
        adminMateService.deleteMate(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }
}
