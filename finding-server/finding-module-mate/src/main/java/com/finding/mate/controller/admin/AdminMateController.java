package com.finding.mate.controller.admin;

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

    /** 编辑邀约(管理员可改一切) */
    @PutMapping("/mates/{id}")
    public Result<Void> updateMate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        MateInvitation m = invitationMapper.selectById(id);
        if (m == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        // 管理端同样走 XSS 清洗 + 违禁词拦截,堵住经后台注入的同类漏洞
        if (body.get("title") != null) m.setTitle(XssUtil.clean(body.get("title").toString()));
        if (body.get("description") != null) m.setDescription(XssUtil.clean(body.get("description").toString()));
        if (body.get("category") != null) m.setCategory((String) body.get("category"));
        if (body.get("location") != null) m.setLocation(XssUtil.clean(body.get("location").toString()));
        sensitiveWordFilter.assertClean(m.getTitle(), m.getDescription(), m.getLocation());
        if (body.get("activityTime") != null && !body.get("activityTime").toString().isEmpty()) {
            try {
                m.setActivityTime(LocalDateTime.parse(body.get("activityTime").toString().replace(' ', 'T')));
            } catch (Exception ignored) {}
        }
        if (body.get("maxParticipants") != null) m.setMaxParticipants(((Number) body.get("maxParticipants")).intValue());
        invitationMapper.updateById(m);
        return Result.ok();
    }

    /** 下架邀约：status=0(软删，列表页自动隐藏) */
    @PutMapping("/mates/{id}/status")
    public Result<Void> updateMateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        Integer status = body.get("status");
        if (status == null) throw new BusinessException(ResultCode.PARAM_ERROR, "status 必填");
        invitation.setStatus(status);
        invitationMapper.updateById(invitation);
        operationAuditService.record(JwtInterceptor.getCurrentUserId(), "mate_status", "mate", invitation.getId(),
                "管理员变更搭子状态", "status=" + status);
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
    public Result<Void> reviewMate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        Boolean pass = body.get("pass") != null && Boolean.parseBoolean(body.get("pass").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        Long adminId = JwtInterceptor.getCurrentUserId();

        invitation.setReviewStatus(pass ? 0 : 2);
        invitation.setReviewReason(pass ? null : reason);
        invitation.setReviewBy(adminId);
        invitation.setReviewTime(LocalDateTime.now());
        invitationMapper.updateById(invitation);

        if (!pass && invitation.getUserId() != null) {
            String content = reason != null && !reason.isBlank() ? "你的搭子邀约审核未通过：" + reason : "你的搭子邀约审核未通过";
            messageService.notify(adminId, invitation.getUserId(), "mate_rejected_review", content, invitation.getId());
        }
        operationAuditService.record(adminId, "mate_review", "mate", invitation.getId(),
                pass ? "搭子审核通过" : "搭子审核拒绝", reason);
        return Result.ok();
    }

    /** 物理删除邀约，级联清理参与者 */
    @DeleteMapping("/mates/{id}")
    @Transactional
    public Result<Void> deleteMate(@PathVariable Long id) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        participantMapper.delete(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id));
        invitationMapper.deleteById(id);
        return Result.ok();
    }

    private String categoryLabel(String code) {
        if (!StringUtils.hasText(code)) return "";
        for (MateCategoryEnum e : MateCategoryEnum.values()) {
            if (e.getCode().equals(code)) return e.getDesc();
        }
        return code;
    }
}
