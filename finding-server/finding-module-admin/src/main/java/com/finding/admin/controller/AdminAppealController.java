package com.finding.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.post.entity.Appeal;
import com.finding.post.entity.Post;
import com.finding.post.mapper.AppealMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtInterceptor;
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
import java.util.Set;

/** 管理员 - 申诉处理:通过则重新发布内容,驳回则保留原结果 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAppealController {

    private final AppealMapper appealMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final OperationAuditService operationAuditService;

    @GetMapping("/appeals")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Appeal::getStatus, status);
        wrapper.orderByDesc(Appeal::getCreatedAt);
        Page<Appeal> result = appealMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> userIds = result.getRecords().stream().map(Appeal::getUserId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }
        List<Map<String, Object>> records = result.getRecords().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("userId", a.getUserId());
            m.put("userNickname", nicknameMap.getOrDefault(a.getUserId(), "用户" + a.getUserId()));
            m.put("targetType", a.getTargetType());
            m.put("targetId", a.getTargetId());
            m.put("reason", a.getReason());
            m.put("originalResult", a.getOriginalResult());
            m.put("status", a.getStatus());
            m.put("handleNote", a.getHandleNote());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).toList();
        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 处理申诉:pass=true 通过(重新发布),false 驳回(保留原结果) */
    @PutMapping("/appeals/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Appeal a = appealMapper.selectById(id);
        if (a == null) throw new BusinessException(ResultCode.PARAM_ERROR, "申诉不存在");
        Boolean pass = body.get("pass") != null && Boolean.parseBoolean(body.get("pass").toString());
        String note = body.get("note") != null ? body.get("note").toString() : null;
        Long adminId = JwtInterceptor.getCurrentUserId();

        a.setStatus(pass ? 1 : 2);
        a.setHandleBy(adminId);
        a.setHandleNote(note);
        a.setHandleTime(LocalDateTime.now());
        appealMapper.updateById(a);

        // 通过 → 重新发布:同时清审核拒绝态与被下架态(status 2 → 1 正常)
        if (pass && "post".equals(a.getTargetType())) {
            Post post = postMapper.selectById(a.getTargetId());
            if (post != null) {
                post.setReviewStatus(0);
                post.setReviewReason(null);
                if (post.getStatus() != null && post.getStatus() == 2) {
                    post.setStatus(1);
                }
                postMapper.updateById(post);
            }
        }
        messageService.notify(adminId, a.getUserId(),
                pass ? "appeal_approved" : "appeal_rejected",
                pass ? "你的申诉已通过，动态已重新发布" : ("申诉未通过" + (note != null && !note.isBlank() ? "：" + note : "")),
                a.getTargetId());
        operationAuditService.record(adminId, "appeal_handle", "appeal", a.getId(), pass ? "申诉通过" : "申诉驳回", note);
        return Result.ok();
    }
}
