package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminLoveGuideService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.post.entity.LoveGuide;
import com.finding.post.mapper.LoveGuideMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminLoveGuideServiceImpl implements AdminLoveGuideService {

    private final LoveGuideMapper loveGuideMapper;
    private final MessageService messageService;
    private final OperationAuditService operationAuditService;

    @Override
    public PageVO<LoveGuide> queue(int page, int size) {
        size = AdminPaging.clampSize(size);
        // 展示全部投稿(待审核/已通过/已拒绝),按提交时间倒序,便于管理端查看历史
        Page<LoveGuide> result = loveGuideMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<LoveGuide>().orderByDesc(LoveGuide::getCreatedAt));
        return PageVO.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public void review(Long adminId, Long id, Map<String, Object> body) {
        LoveGuide guide = loveGuideMapper.selectById(id);
        if (guide == null) throw new BusinessException(ResultCode.PARAM_ERROR, "内容不存在");

        boolean pass = body.get("pass") != null && Boolean.parseBoolean(body.get("pass").toString());
        String reason = body.get("reason") == null ? null : body.get("reason").toString();

        guide.setReviewStatus(pass ? 1 : 2);
        guide.setReviewReason(pass ? null : reason);
        guide.setReviewBy(adminId);
        guide.setReviewTime(LocalDateTime.now());
        loveGuideMapper.updateById(guide);

        if (!pass) {
            messageService.notify(adminId, guide.getUserId(), "love_guide_rejected",
                    reason == null || reason.isBlank()
                            ? "你的恋爱经验投稿未通过审核"
                            : "你的恋爱经验投稿未通过审核：" + reason,
                    id);
        }
        operationAuditService.record(adminId, "love_guide_review", "love_guide", id,
                pass ? "审核通过" : "审核拒绝", reason);
    }
}
