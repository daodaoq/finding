package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminFeedbackService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.feedback.Feedback;
import com.finding.common.feedback.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final AdminUserLookupService userLookupService;

    @Override
    public PageVO<Map<String, Object>> listFeedbacks(int page, int size, Integer status) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Feedback::getStatus, status);
        }
        wrapper.orderByDesc(Feedback::getCreatedAt);
        Page<Feedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> uids = result.getRecords().stream().map(Feedback::getUserId).distinct().toList();
        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(uids);

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
        }).toList();

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updateStatus(Long id, Map<String, Integer> body) {
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
    }
}
