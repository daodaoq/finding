package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminAppealService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.post.entity.Appeal;
import com.finding.post.entity.Post;
import com.finding.post.mapper.AppealMapper;
import com.finding.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAppealServiceImpl implements AdminAppealService {

    private final AppealMapper appealMapper;
    private final PostMapper postMapper;
    private final AdminUserLookupService userLookupService;
    private final MessageService messageService;
    private final OperationAuditService operationAuditService;

    @Override
    public PageVO<Map<String, Object>> listAppeals(int page, int size, Integer status) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Appeal::getStatus, status);
        wrapper.orderByDesc(Appeal::getCreatedAt);
        Page<Appeal> result = appealMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(result.getRecords().stream()
                .map(Appeal::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));

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
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void handle(Long adminId, Long id, Map<String, Object> body) {
        Appeal a = appealMapper.selectById(id);
        if (a == null) throw new BusinessException(ResultCode.PARAM_ERROR, "申诉不存在");
        Boolean pass = body.get("pass") != null && Boolean.parseBoolean(body.get("pass").toString());
        String note = body.get("note") != null ? body.get("note").toString() : null;

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
    }
}
