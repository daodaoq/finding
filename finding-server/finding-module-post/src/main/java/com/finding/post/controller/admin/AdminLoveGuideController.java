package com.finding.post.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.post.entity.LoveGuide;
import com.finding.post.mapper.LoveGuideMapper;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/love-guides")
@RequiredArgsConstructor
public class AdminLoveGuideController {
    private final LoveGuideMapper mapper; private final MessageService messageService; private final OperationAuditService audit;
    @GetMapping("/review")
    public Result<PageVO<LoveGuide>> queue(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size) {
        Page<LoveGuide> r = mapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<LoveGuide>().eq(LoveGuide::getReviewStatus, 0).orderByAsc(LoveGuide::getCreatedAt));
        return Result.ok(PageVO.of(r.getRecords(), r.getTotal(), page, size));
    }
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LoveGuide guide = mapper.selectById(id); if (guide == null) throw new BusinessException(ResultCode.PARAM_ERROR, "内容不存在");
        boolean pass = body.get("pass") != null && Boolean.parseBoolean(body.get("pass").toString());
        String reason = body.get("reason") == null ? null : body.get("reason").toString(); Long adminId = JwtInterceptor.getCurrentUserId();
        guide.setReviewStatus(pass ? 1 : 2); guide.setReviewReason(pass ? null : reason); guide.setReviewBy(adminId); guide.setReviewTime(LocalDateTime.now()); mapper.updateById(guide);
        if (!pass) messageService.notify(adminId, guide.getUserId(), "love_guide_rejected", reason == null || reason.isBlank() ? "你的恋爱经验投稿未通过审核" : "你的恋爱经验投稿未通过审核：" + reason, id);
        audit.record(adminId, "love_guide_review", "love_guide", id, pass ? "审核通过" : "审核拒绝", reason); return Result.ok();
    }
}
