package com.finding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.entity.User;
import com.finding.entity.UserVerification;
import com.finding.mapper.UserMapper;
import com.finding.mapper.UserVerificationMapper;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员接口 —— 用户管理、认证审核等。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserVerificationMapper verificationMapper;
    private final UserMapper userMapper;

    /** 认证审核列表（按状态筛选，默认待审核） */
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

        // 附加用户手机号
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

    /** 通过认证 */
    @PutMapping("/verifications/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "0") Long reviewerId) {
        UserVerification v = verificationMapper.selectById(id);
        if (v == null) throw new BusinessException(ResultCode.PARAM_ERROR, "认证记录不存在");
        if (v.getStatus() != 0) throw new BusinessException(ResultCode.PARAM_ERROR, "该认证已处理");

        v.setStatus(1);
        v.setReviewerId(reviewerId);
        verificationMapper.updateById(v);

        // 更新用户认证状态
        User user = userMapper.selectById(v.getUserId());
        if (user != null) {
            user.setRealNameVerified(2);   // approved
            user.setStudentId(v.getStudentId());
            userMapper.updateById(user);
        }

        return Result.ok();
    }

    /** 拒绝认证 */
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

        // 更新用户认证状态
        User user = userMapper.selectById(v.getUserId());
        if (user != null) {
            user.setRealNameVerified(3);   // rejected
            userMapper.updateById(user);
        }

        return Result.ok();
    }
}
