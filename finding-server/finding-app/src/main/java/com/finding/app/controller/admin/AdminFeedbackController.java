package com.finding.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.app.entity.Feedback;
import com.finding.app.mapper.FeedbackMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员 - 用户反馈/客服工单处理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;

    @GetMapping("/feedbacks")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Feedback::getStatus, status);
        }
        wrapper.orderByDesc(Feedback::getCreatedAt);
        Page<Feedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> uids = result.getRecords().stream().map(Feedback::getUserId).distinct().toList();
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!uids.isEmpty()) {
            userMapper.selectBatchIds(uids).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("userId", f.getUserId());
            m.put("nickname", nicknameMap.getOrDefault(f.getUserId(), ""));
            m.put("type", f.getType());
            m.put("content", f.getContent());
            m.put("contact", f.getContact());
            m.put("status", f.getStatus());
            m.put("createdAt", f.getCreatedAt());
            m.put("handledAt", f.getHandledAt());
            return m;
        }).collect(Collectors.toList());

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 标记工单为已处理(1)/重新打开(0) */
    @PutMapping("/feedbacks/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Feedback f = feedbackMapper.selectById(id);
        if (f == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "工单不存在");
        }
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态参数不合法");
        }
        f.setStatus(status);
        f.setHandledAt(status == 1 ? LocalDateTime.now() : null);
        feedbackMapper.updateById(f);
        return Result.ok();
    }
}
