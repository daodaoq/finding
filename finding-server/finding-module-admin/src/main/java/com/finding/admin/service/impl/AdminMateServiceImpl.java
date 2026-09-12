package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminMateService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.common.constant.MateCategoryEnum;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.mate.constant.MateInvitationStatus;
import com.finding.mate.constant.MateParticipantStatus;
import com.finding.mate.dto.AdminMateReviewDTO;
import com.finding.mate.dto.AdminMateStatusDTO;
import com.finding.mate.dto.AdminMateUpdateDTO;
import com.finding.mate.entity.MateInvitation;
import com.finding.mate.entity.MateParticipant;
import com.finding.mate.mapper.MateInvitationMapper;
import com.finding.mate.mapper.MateParticipantMapper;
import com.finding.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMateServiceImpl implements AdminMateService {

    private final MateInvitationMapper invitationMapper;
    private final MateParticipantMapper participantMapper;
    private final AdminUserLookupService userLookupService;
    private final OperationAuditService operationAuditService;
    private final MessageService messageService;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    public PageVO<Map<String, Object>> listMates(int page, int size, String keyword, Integer status) {
        size = AdminPaging.clampSize(size);
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
        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(
                result.getRecords().stream().map(MateInvitation::getUserId).collect(Collectors.toSet()));

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

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updateMate(Long id, AdminMateUpdateDTO body) {
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
    }

    @Override
    @Transactional
    public void updateMateStatus(Long adminId, Long id, AdminMateStatusDTO body) {
        Integer status = body.getStatus();
        if (status == null
                || (status != MateInvitationStatus.CANCELLED.getCode() && status != MateInvitationStatus.CLOSED.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "管理员仅可将进行中活动下架或关闭");
        }
        boolean cancelled = status == MateInvitationStatus.CANCELLED.getCode();
        applyStatus(adminId, id, status,
                cancelled ? "搭子活动已由平台下架" : "搭子活动已停止接受报名",
                "管理员变更搭子状态");
    }

    @Override
    @Transactional
    public void closeMate(Long adminId, Long id) {
        applyStatus(adminId, id, MateInvitationStatus.CLOSED.getCode(), "管理员关闭搭子活动", "管理员关闭搭子活动");
    }

    @Override
    @Transactional
    public void cancelMate(Long adminId, Long id) {
        applyStatus(adminId, id, MateInvitationStatus.CANCELLED.getCode(), "管理员取消搭子活动", "管理员取消搭子活动");
    }

    /** 统一状态流转:仅「进行中 → 已取消/已关闭」,并连带通知/作废报名者(事务由调用方开启) */
    private void applyStatus(Long adminId, Long id, int targetStatus, String notice, String auditAction) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有进行中的活动可以执行该操作");
        }
        invitation.setStatus(targetStatus);
        invitationMapper.updateById(invitation);
        boolean cancelled = targetStatus == MateInvitationStatus.CANCELLED.getCode();
        invalidateParticipants(adminId, invitation, cancelled, notice);
        operationAuditService.record(adminId, "mate_status", "mate", id, auditAction, "status=" + targetStatus);
    }

    @Override
    public PageVO<Map<String, Object>> reviewQueue(int page, int size) {
        size = AdminPaging.clampSize(size);
        Page<MateInvitation> result = invitationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MateInvitation>()
                        .eq(MateInvitation::getReviewStatus, 1)
                        .orderByAsc(MateInvitation::getCreatedAt));
        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(result.getRecords().stream()
                .map(MateInvitation::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<Map<String, Object>> records = result.getRecords().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("description", m.getDescription());
            map.put("userNickname", nicknameMap.getOrDefault(m.getUserId(), "用户" + m.getUserId()));
            map.put("createdAt", m.getCreatedAt());
            return map;
        }).toList();
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void reviewMate(Long adminId, Long id, AdminMateReviewDTO body) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        Boolean pass = body.getPass();
        String reason = XssUtil.clean(body.getReason());
        if (!pass && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "拒绝审核时必须填写原因");
        }

        // 带 reviewStatus=1 条件更新,避免两个管理员同时处理同一活动
        int rows = invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                .eq(MateInvitation::getId, id)
                .eq(MateInvitation::getReviewStatus, 1)
                .set(MateInvitation::getReviewStatus, pass ? 0 : 2)
                .set(MateInvitation::getReviewReason, pass ? null : reason)
                .set(MateInvitation::getReviewBy, adminId)
                .set(MateInvitation::getReviewTime, LocalDateTime.now()));
        if (rows == 0) throw new BusinessException(ResultCode.PARAM_ERROR, "该活动已被其他管理员处理");

        if (invitation.getUserId() != null) {
            if (pass) {
                messageService.notify(adminId, invitation.getUserId(), "mate_approved_review",
                        "你的搭子邀约已审核通过，现已对外展示", invitation.getId());
            } else {
                String content = StringUtils.hasText(reason)
                        ? "你的搭子邀约审核未通过：" + reason
                        : "你的搭子邀约审核未通过";
                messageService.notify(adminId, invitation.getUserId(), "mate_rejected_review", content, invitation.getId());
            }
        }
        operationAuditService.record(adminId, "mate_review", "mate", invitation.getId(),
                pass ? "搭子审核通过" : "搭子审核拒绝", reason);
    }

    @Override
    @Transactional
    public void deleteMate(Long adminId, Long id) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        invitation.setStatus(MateInvitationStatus.CANCELLED.getCode());
        invitationMapper.updateById(invitation);
        invalidateParticipants(adminId, invitation, true, "搭子活动已由平台下架");
        operationAuditService.record(adminId, "mate_delete", "mate", id, "管理员软删除搭子活动", null);
    }

    /** 作废报名者(待处理/候补,取消时含已通过),并逐一通知 */
    private void invalidateParticipants(Long adminId, MateInvitation invitation, boolean includeAccepted, String notice) {
        List<Integer> statuses = new ArrayList<>(List.of(
                MateParticipantStatus.PENDING.getCode(), MateParticipantStatus.WAITLISTED.getCode()));
        if (includeAccepted) statuses.add(MateParticipantStatus.ACCEPTED.getCode());
        List<MateParticipant> affected = participantMapper.selectList(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, invitation.getId())
                .in(MateParticipant::getStatus, statuses));
        for (MateParticipant participant : affected) {
            messageService.notify(adminId, participant.getUserId(),
                    "mate_admin_closed", notice, invitation.getId());
        }
        participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
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
