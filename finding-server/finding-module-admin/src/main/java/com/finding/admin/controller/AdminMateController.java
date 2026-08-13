package com.finding.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.common.PageVO;
import com.finding.common.constant.MateCategoryEnum;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.mate.entity.MateInvitation;
import com.finding.mate.entity.MateParticipant;
import com.finding.mate.dto.AdminMateUpdateDTO;
import com.finding.mate.dto.AdminMateStatusDTO;
import com.finding.mate.dto.AdminMateReviewDTO;
import com.finding.mate.constant.MateInvitationStatus;
import com.finding.mate.constant.MateParticipantStatus;
import com.finding.mate.mapper.MateInvitationMapper;
import com.finding.mate.mapper.MateParticipantMapper;
import com.finding.message.service.MessageService;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员 - 搭子邀约管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMateController {

    private final MateInvitationMapper invitationMapper;
    private final MateParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final OperationAuditService operationAuditService;
    private final MessageService messageService;
    private final SensitiveWordFilter sensitiveWordFilter;

    @GetMapping("/mates")
    public Result<PageVO<Map<String, Object>>> listMates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<MateInvitation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(MateInvitation::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MateInvitation::getTitle, keyword)
                    .or().like(MateInvitation::getDescription, keyword));
        }
        wrapper.orderByDesc(MateInvitation::getCreatedAt);

        Page<MateInvitation> result = invitationMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量取发起人昵称
        Set<Long> userIds = result.getRecords().stream()
                .map(MateInvitation::getUserId).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("category", m.getCategory());
            map.put("categoryLabel", categoryLabel(m.getCategory()));
            map.put("creatorNickname", nicknameMap.getOrDefault(m.getUserId(), "用户" + m.getUserId()));
            map.put("maxParticipants", m.getMaxParticipants());
            map.put("currentParticipants", m.getCurrentParticipants());
            map.put("activityTime", m.getActivityTime());
            map.put("location", m.getLocation());
            map.put("status", m.getStatus());
            map.put("createdAt", m.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 管理员编辑邀约：强类型校验，禁止绕过人数、时间与内容约束。 */
    @PutMapping("/mates/{id}")
    public Result<Void> updateMate(@PathVariable Long id, @Valid @RequestBody AdminMateUpdateDTO body) {
        MateInvitation m = invitationMapper.selectById(id);
        if (m == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        if (body.getMaxParticipants() < m.getCurrentParticipants()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "人数上限不能低于当前已确认人数");
        }
        if (body.getActivityTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动时间不能早于当前时间");
        }
        m.setTitle(XssUtil.clean(body.getTitle()));
        m.setDescription(XssUtil.clean(body.getDescription()));
        m.setCategory(body.getCategory());
        m.setLocation(XssUtil.clean(body.getLocation()));
        sensitiveWordFilter.assertClean(m.getTitle(), m.getDescription(), m.getLocation());
        m.setActivityTime(body.getActivityTime());
        m.setMaxParticipants(body.getMaxParticipants());
        invitationMapper.updateById(m);
        return Result.ok();
    }

    /** 下架邀约：status=0(软删，列表页自动隐藏) */
    @PutMapping("/mates/{id}/status")
    public Result<Void> updateMateStatus(@PathVariable Long id, @Valid @RequestBody AdminMateStatusDTO body) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        Integer status = body.getStatus();
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode()
                || (status != MateInvitationStatus.CANCELLED.getCode() && status != MateInvitationStatus.CLOSED.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "管理员仅可将进行中活动下架或关闭");
        }
        invitation.setStatus(status);
        invitationMapper.updateById(invitation);
        invalidateParticipants(invitation, status == MateInvitationStatus.CANCELLED.getCode(),
                status == MateInvitationStatus.CANCELLED.getCode() ? "搭子活动已由平台下架" : "搭子活动已停止接受报名");
        operationAuditService.record(JwtInterceptor.getCurrentUserId(), "mate_status", "mate", invitation.getId(),
                "管理员变更搭子状态", "status=" + status);
        return Result.ok();
    }

    @PutMapping("/mates/{id}/close")
    public Result<Void> closeMate(@PathVariable Long id) {
        return setAdminStatus(id, MateInvitationStatus.CLOSED.getCode(), "管理员关闭搭子活动");
    }

    @PutMapping("/mates/{id}/cancel")
    public Result<Void> cancelMate(@PathVariable Long id) {
        return setAdminStatus(id, MateInvitationStatus.CANCELLED.getCode(), "管理员取消搭子活动");
    }

    private Result<Void> setAdminStatus(Long id, int status, String action) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有进行中的活动可以执行该操作");
        }
        invitation.setStatus(status);
        invitationMapper.updateById(invitation);
        invalidateParticipants(invitation, status == MateInvitationStatus.CANCELLED.getCode(), action);
        operationAuditService.record(JwtInterceptor.getCurrentUserId(), "mate_status", "mate", id, action, null);
        return Result.ok();
    }

    /** 搭子待审核队列(review_status=1) */
    @GetMapping("/mates/review")
    public Result<PageVO<Map<String, Object>>> reviewQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MateInvitation> result = invitationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MateInvitation>()
                        .eq(MateInvitation::getReviewStatus, 1)
                        .orderByAsc(MateInvitation::getCreatedAt));
        Set<Long> userIds = result.getRecords().stream().map(MateInvitation::getUserId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }
        List<Map<String, Object>> records = result.getRecords().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("description", m.getDescription());
            map.put("userNickname", nicknameMap.getOrDefault(m.getUserId(), "用户" + m.getUserId()));
            map.put("createdAt", m.getCreatedAt());
            return map;
        }).toList();
        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 搭子审核处理:pass=true 通过发布,false 拒绝(需 reason,通知作者) */
    @PutMapping("/mates/{id}/review")
    public Result<Void> reviewMate(@PathVariable Long id, @Valid @RequestBody AdminMateReviewDTO body) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        Boolean pass = body.getPass();
        String reason = XssUtil.clean(body.getReason());
        if (!pass && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "拒绝审核时必须填写原因");
        }
        Long adminId = JwtInterceptor.getCurrentUserId();

        invitation.setReviewStatus(pass ? 0 : 2);
        invitation.setReviewReason(pass ? null : reason);
        invitation.setReviewBy(adminId);
        invitation.setReviewTime(LocalDateTime.now());
        int rows = invitationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MateInvitation>()
                .eq(MateInvitation::getId, id)
                .eq(MateInvitation::getReviewStatus, 1)
                .set(MateInvitation::getReviewStatus, pass ? 0 : 2)
                .set(MateInvitation::getReviewReason, pass ? null : reason)
                .set(MateInvitation::getReviewBy, adminId)
                .set(MateInvitation::getReviewTime, LocalDateTime.now()));
        if (rows == 0) throw new BusinessException(ResultCode.PARAM_ERROR, "该活动已被其他管理员处理");

        if (!pass && invitation.getUserId() != null) {
            String content = reason != null && !reason.isBlank() ? "你的搭子邀约审核未通过：" + reason : "你的搭子邀约审核未通过";
            messageService.notify(adminId, invitation.getUserId(), "mate_rejected_review", content, invitation.getId());
        } else if (pass && invitation.getUserId() != null) {
            messageService.notify(adminId, invitation.getUserId(), "mate_approved_review", "你的搭子邀约已审核通过，现已对外展示", invitation.getId());
        }
        operationAuditService.record(adminId, "mate_review", "mate", invitation.getId(),
                pass ? "搭子审核通过" : "搭子审核拒绝", reason);
        return Result.ok();
    }

    /** 软删除邀约，保留参与、举报和审核审计记录。 */
    @DeleteMapping("/mates/{id}")
    @Transactional
    public Result<Void> deleteMate(@PathVariable Long id) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        invitation.setStatus(MateInvitationStatus.CANCELLED.getCode());
        invitationMapper.updateById(invitation);
        invalidateParticipants(invitation, true, "搭子活动已由平台下架");
        operationAuditService.record(JwtInterceptor.getCurrentUserId(), "mate_delete", "mate", id,
                "管理员软删除搭子活动", null);
        return Result.ok();
    }

    private void invalidateParticipants(MateInvitation invitation, boolean includeAccepted, String notice) {
        List<Integer> statuses = new ArrayList<>(List.of(
                MateParticipantStatus.PENDING.getCode(), MateParticipantStatus.WAITLISTED.getCode()));
        if (includeAccepted) statuses.add(MateParticipantStatus.ACCEPTED.getCode());
        List<MateParticipant> affected = participantMapper.selectList(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, invitation.getId())
                .in(MateParticipant::getStatus, statuses));
        for (MateParticipant participant : affected) {
            messageService.notify(JwtInterceptor.getCurrentUserId(), participant.getUserId(),
                    "mate_admin_closed", notice, invitation.getId());
        }
        participantMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, invitation.getId())
                .in(MateParticipant::getStatus, statuses)
                .set(MateParticipant::getStatus, MateParticipantStatus.INVALIDATED.getCode()));
    }

    private String categoryLabel(String code) {
        if (!StringUtils.hasText(code)) return "";
        for (MateCategoryEnum e : MateCategoryEnum.values()) {
            if (e.getCode().equals(code)) return e.getDesc();
        }
        return code;
    }
}
