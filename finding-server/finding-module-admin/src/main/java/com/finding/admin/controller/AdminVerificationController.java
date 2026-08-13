package com.finding.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.user.entity.User;
import com.finding.user.entity.UserVerification;
import com.finding.user.event.UserVerifiedEvent;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserVerificationMapper;
import com.finding.user.security.JwtInterceptor;
import com.finding.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 学生认证审核。
 * 审核结果通过事件通知被审核用户(见 app 模块 UserVerifiedListener)。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminVerificationController {

    private final UserVerificationMapper verificationMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/verifications")
    public Result<PageVO<Map<String, Object>>> listVerifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<UserVerification> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(UserVerification::getStatus, status);
        }
        wrapper.orderByDesc(UserVerification::getCreatedAt);

        Page<UserVerification> result = verificationMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> userIds = result.getRecords().stream()
                .map(UserVerification::getUserId).distinct().toList();
        Map<Long, String> phoneMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> phoneMap.put(u.getId(), u.getPhone()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(v -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId());
            map.put("userId", v.getUserId());
            map.put("phone", phoneMap.getOrDefault(v.getUserId(), ""));
            map.put("realName", v.getRealName());
            map.put("studentId", v.getStudentId());
            map.put("school", v.getSchool());
            map.put("idCardFront", v.getIdCardFront());
            map.put("idCardBack", v.getIdCardBack());
            map.put("studentCard", v.getStudentCard());
            map.put("status", v.getStatus());
            map.put("reviewComment", v.getReviewComment());
            map.put("createdAt", v.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    @PutMapping("/verifications/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "0") Long reviewerId) {
        UserVerification v = verificationMapper.selectById(id);
        if (v == null) throw new BusinessException(ResultCode.PARAM_ERROR, "认证记录不存在");
        if (v.getStatus() != 0) throw new BusinessException(ResultCode.PARAM_ERROR, "该认证已处理");

        v.setStatus(1);
        v.setReviewerId(reviewerId);
        verificationMapper.updateById(v);

        User user = userMapper.selectById(v.getUserId());
        if (user != null) {
            user.setRealNameVerified(2);
            user.setStudentId(v.getStudentId());
            userMapper.updateById(user);
        }

        // 通知用户认证已通过
        eventPublisher.publishEvent(new UserVerifiedEvent(
                v.getUserId(), true, null, JwtInterceptor.getCurrentUserId()));
        return Result.ok();
    }

    @PutMapping("/verifications/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "0") Long reviewerId,
                                @RequestParam(defaultValue = "") String comment) {
        UserVerification v = verificationMapper.selectById(id);
        if (v == null) throw new BusinessException(ResultCode.PARAM_ERROR, "认证记录不存在");
        if (v.getStatus() != 0) throw new BusinessException(ResultCode.PARAM_ERROR, "该认证已处理");

        v.setStatus(2);
        v.setReviewerId(reviewerId);
        v.setReviewComment(comment);
        verificationMapper.updateById(v);

        User user = userMapper.selectById(v.getUserId());
        if (user != null) {
            user.setRealNameVerified(3);
            userMapper.updateById(user);
        }

        // 通知用户认证未通过及原因
        eventPublisher.publishEvent(new UserVerifiedEvent(
                v.getUserId(), false, comment, JwtInterceptor.getCurrentUserId()));
        return Result.ok();
    }
}
