package com.finding.mate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.mate.constant.MateInvitationStatus;
import com.finding.mate.constant.MateParticipantStatus;
import com.finding.mate.dto.MateCreateDTO;
import com.finding.mate.dto.MateQueryDTO;


import com.finding.common.word.ReviewResult;
import com.finding.mate.service.MateService;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserService;
import com.finding.user.service.UserWriteGuard;
import com.finding.common.GeoUtils;
import com.finding.mate.vo.MateVO;
import com.finding.common.PageVO;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.common.event.UserBlockedEvent;
import com.finding.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.finding.mate.entity.MateInvitation;
import com.finding.mate.entity.MateParticipant;
import com.finding.mate.mapper.MateInvitationMapper;
import com.finding.mate.mapper.MateParticipantMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.message.service.MessageService;

@Service
@RequiredArgsConstructor
public class MateServiceImpl implements MateService {

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            "travel", "carpool", "fitness", "study", "exam", "sports", "gaming", "entertainment", "other");

    private final MateInvitationMapper invitationMapper;
    private final MateParticipantMapper participantMapper;
    private final MessageService messageService;
    private final UserMapper userMapper;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final UserRelationshipService relationshipService;
    private final UserWriteGuard userWriteGuard;

    @Override
    public PageVO<MateVO> listInvitations(MateQueryDTO query, Long currentUserId) {
        // 公开可见性:进行中 + 已发布 + 未过期 + 排除被拉黑发起人(与全局搜索共用同一条件)
        LambdaQueryWrapper<MateInvitation> wrapper = new LambdaQueryWrapper<>();
        applyPublicFilter(wrapper, currentUserId);

        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(MateInvitation::getCategory, query.getCategory());
        }
        if (StringUtils.hasText(query.getCity())) {
            wrapper.like(MateInvitation::getLocation, query.getCity().trim());
        }
        if (Boolean.TRUE.equals(query.getAnonymousOnly())) {
            wrapper.eq(MateInvitation::getIsAnonymous, 1);
        }
        if (query.getDaysAhead() != null && query.getDaysAhead() > 0) {
            wrapper.le(MateInvitation::getActivityTime, LocalDateTime.now().plusDays(Math.min(query.getDaysAhead(), 30)));
        }
        if (Boolean.TRUE.equals(query.getAvailableOnly())) {
            wrapper.apply("current_participants < max_participants");
        }
        applyGeoBounds(wrapper, query);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(MateInvitation::getTitle, query.getKeyword())
                    .or().like(MateInvitation::getDescription, query.getKeyword()));
        }
        wrapper.orderByAsc(MateInvitation::getActivityTime); // 时间最近优先（升序）

        Page<MateInvitation> page = new Page<>(query.getPage(), query.getSize());
        Page<MateInvitation> result = invitationMapper.selectPage(page, wrapper);

        Double lat = query.getLatitude() != null ? query.getLatitude().doubleValue() : null;
        Double lng = query.getLongitude() != null ? query.getLongitude().doubleValue() : null;
        List<MateVO> records = result.getRecords().stream()
                .map(m -> toVO(m, currentUserId, lat, lng))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public PageVO<Map<String, Object>> searchInvitations(Long currentUserId, String keyword, int page, int size) {
        LambdaQueryWrapper<MateInvitation> wrapper = new LambdaQueryWrapper<>();
        applyPublicFilter(wrapper, currentUserId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MateInvitation::getTitle, keyword);
        }
        wrapper.orderByDesc(MateInvitation::getCreatedAt);

        Page<MateInvitation> matePage = invitationMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> records = matePage.getRecords().stream().map(mv -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", mv.getId());
            m.put("title", mv.getTitle());
            m.put("category", mv.getCategory());
            m.put("location", "报名通过后可查看集合地点");
            m.put("activityTime", mv.getActivityTime());
            m.put("createdAt", mv.getCreatedAt());
            // 匿名活动不返回发起人 ID
            m.put("userId", mv.getIsAnonymous() != null && mv.getIsAnonymous() == 1 ? null : mv.getUserId());
            return m;
        }).toList();
        return PageVO.of(records, matePage.getTotal(), page, size);
    }

    /**
     * 公开可见性过滤:活动进行中 + 已发布(审核通过) + 未过期 + 排除被拉黑发起人。
     * 搭子列表与全局搜索共同复用,保证两处可见性一致。
     */
    private void applyPublicFilter(LambdaQueryWrapper<MateInvitation> wrapper, Long currentUserId) {
        wrapper.eq(MateInvitation::getStatus, MateInvitationStatus.ACTIVE.getCode())
                .eq(MateInvitation::getReviewStatus, 0)
                .ge(MateInvitation::getActivityTime, LocalDateTime.now());
        if (currentUserId != null) {
            Set<Long> blocked = relationshipService.blockedUserIds(currentUserId);
            if (blocked != null && !blocked.isEmpty()) {
                wrapper.notIn(MateInvitation::getUserId, blocked);
            }
        }
    }

    @Override
    public MateVO getInvitationDetail(Long id, Long currentUserId) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        // 审核可见性:待审/拒绝仅作者可看
        Integer rs = invitation.getReviewStatus() != null ? invitation.getReviewStatus() : 0;
        if (rs != 0 && (currentUserId == null || !invitation.getUserId().equals(currentUserId))) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        return toVO(invitation, currentUserId, null, null);
    }

    @Override
    @Transactional
    public MateVO createInvitation(Long userId, MateCreateDTO dto) {
        userWriteGuard.checkWritable(userId);
        validateInvitationRules(dto);
        // 统一内容准备:先清洗后赋值 + 拦截/送审分类(标题/描述/地点一并清洗)
        PreparedContent pc = prepareInvitationContent(dto);
        MateInvitation invitation = new MateInvitation();
        invitation.setUserId(userId);
        invitation.setCategory(dto.getCategory());
        invitation.setTitle(pc.title());
        invitation.setDescription(pc.description());
        invitation.setActivityTime(dto.getActivityTime());
        invitation.setLocation(pc.location());
        invitation.setLatitude(dto.getLatitude());
        invitation.setLongitude(dto.getLongitude());
        invitation.setMaxParticipants(dto.getMaxParticipants());
        invitation.setCurrentParticipants(1);
        invitation.setIsAnonymous(dto.getIsAnonymous());
        invitation.setStatus(1);
        invitation.setReviewStatus(pc.review().hasReview() ? 1 : 0);
        invitationMapper.insert(invitation);
        return toVO(invitation, userId, null, null);
    }

    /** 清洗 + 审核分类结果 */
    private record PreparedContent(String title, String description, String location, ReviewResult review) {
    }

    /** 统一内容准备:XSS 清洗 + 拦截/送审分类(创建与编辑共用;先清洗,命中拦截词直接拒绝) */
    private PreparedContent prepareInvitationContent(MateCreateDTO dto) {
        String title = XssUtil.clean(dto.getTitle() != null ? dto.getTitle() : "");
        String description = XssUtil.clean(dto.getDescription() != null ? dto.getDescription() : "");
        String location = XssUtil.clean(dto.getLocation() != null ? dto.getLocation() : "");
        ReviewResult review = sensitiveWordFilter.classifyReview(title, description, location);
        if (review.hasBlocking()) {
            String joined = review.blocking().stream().map(w -> "「" + w + "」").collect(Collectors.joining());
            throw new BusinessException(ResultCode.CONTENT_BLOCKED, "内容包含违禁词:" + joined);
        }
        return new PreparedContent(title, description, location, review);
    }

    private void applyGeoBounds(LambdaQueryWrapper<MateInvitation> wrapper, MateQueryDTO query) {
        if (query.getLatitude() == null || query.getLongitude() == null || query.getRadiusKm() == null
                || query.getRadiusKm() <= 0) return;
        double radius = Math.min(query.getRadiusKm(), 200d);
        double latDelta = radius / 111.0;
        double cos = Math.max(0.1, Math.cos(Math.toRadians(query.getLatitude().doubleValue())));
        double lngDelta = radius / (111.0 * cos);
        wrapper.between(MateInvitation::getLatitude, query.getLatitude().doubleValue() - latDelta,
                        query.getLatitude().doubleValue() + latDelta)
                .between(MateInvitation::getLongitude, query.getLongitude().doubleValue() - lngDelta,
                        query.getLongitude().doubleValue() + lngDelta);
    }

    private void validateInvitationRules(MateCreateDTO dto) {
        if (!StringUtils.hasText(dto.getCategory()) || !SUPPORTED_CATEGORIES.contains(dto.getCategory())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的活动分类");
        }
        if (dto.getMaxParticipants() == null || dto.getMaxParticipants() < 2 || dto.getMaxParticipants() > 50) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "人数上限应在2到50之间");
        }
        if (dto.getIsAnonymous() == null || (dto.getIsAnonymous() != 0 && dto.getIsAnonymous() != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "匿名设置只能为0或1");
        }
        if (dto.getActivityTime() == null || dto.getActivityTime().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动时间至少应晚于当前时间30分钟");
        }
        if ((dto.getLatitude() == null) != (dto.getLongitude() == null)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "经纬度必须同时填写或同时留空");
        }
        if (dto.getLatitude() != null && (dto.getLatitude().doubleValue() < -90 || dto.getLatitude().doubleValue() > 90
                || dto.getLongitude().doubleValue() < -180 || dto.getLongitude().doubleValue() > 180)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "经纬度不在合法范围内");
        }
    }

    @Override
    @Transactional
    public void updateInvitation(Long userId, Long id, MateCreateDTO dto) {
        userWriteGuard.checkWritable(userId);
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        if (!invitation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_CREATOR);
        }
        // 内容拦截优先于普通业务校验，确保违禁内容永远不会进入后续流程。
        PreparedContent pc = prepareInvitationContent(dto);
        validateInvitationRules(dto);
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode() || isExpired(invitation)) {
            throw new BusinessException(ResultCode.MATE_CLOSED, "已取消、已关闭或已过期的活动不能编辑");
        }
        if (dto.getMaxParticipants() < invitation.getCurrentParticipants()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "人数上限不能低于当前已确认人数");
        }
        boolean importantChanged = !java.util.Objects.equals(invitation.getActivityTime(), dto.getActivityTime())
                || !java.util.Objects.equals(invitation.getLocation(), dto.getLocation())
                || !java.util.Objects.equals(invitation.getTitle(), dto.getTitle())
                || !java.util.Objects.equals(invitation.getDescription(), dto.getDescription());
        // 统一内容准备:先清洗后赋值,命中拦截词直接拒绝(数据库不变,不写入未清洗内容)
        invitation.setCategory(dto.getCategory());
        invitation.setTitle(pc.title());
        invitation.setDescription(pc.description());
        invitation.setActivityTime(dto.getActivityTime());
        invitation.setLocation(pc.location());
        invitation.setLatitude(dto.getLatitude());
        invitation.setLongitude(dto.getLongitude());
        invitation.setMaxParticipants(dto.getMaxParticipants());
        // 重新计算审核状态:被拒活动编辑后一律重新送审(不直接恢复公开);其余按是否命中送审词
        Integer oldRs = invitation.getReviewStatus() != null ? invitation.getReviewStatus() : 0;
        if (oldRs == 2) {
            invitation.setReviewStatus(1);
            invitation.setReviewReason(null);
            invitation.setReviewBy(null);
            invitation.setReviewTime(null);
        } else {
            invitation.setReviewStatus(pc.review().hasReview() ? 1 : 0);
        }
        invitationMapper.updateById(invitation);
        if (importantChanged) {
            notifyMembersAndApplicants(invitation, "你报名的搭子活动信息已更新，请查看最新时间、地点和内容");
        }
    }

    @Override
    @Transactional
    public void cancelInvitation(Long userId, Long id) {
        userWriteGuard.checkWritable(userId);
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        if (!invitation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_CREATOR);
        }
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.MATE_CLOSED, "该活动已关闭或取消");
        }
        invitation.setStatus(MateInvitationStatus.CANCELLED.getCode());
        invitationMapper.updateById(invitation);
        // 通知已通过成员 + 待审批/候补申请人
        notifyMembersAndApplicants(invitation, "你参与的搭子活动已取消");
        participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id)
                .in(MateParticipant::getStatus,
                        MateParticipantStatus.ACCEPTED.getCode(),
                        MateParticipantStatus.PENDING.getCode(),
                        MateParticipantStatus.WAITLISTED.getCode())
                .set(MateParticipant::getStatus, MateParticipantStatus.INVALIDATED.getCode()));
    }

    @Override
    @Transactional
    public void closeInvitation(Long userId, Long id) {
        userWriteGuard.checkWritable(userId);
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        if (!invitation.getUserId().equals(userId)) throw new BusinessException(ResultCode.NOT_CREATOR);
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.MATE_CLOSED, "该活动已关闭或取消");
        }
        invitation.setStatus(MateInvitationStatus.CLOSED.getCode());
        invitationMapper.updateById(invitation);
        notifyMembersAndApplicants(invitation, "该搭子活动已停止接受新的报名");
        participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id)
                .in(MateParticipant::getStatus,
                        MateParticipantStatus.PENDING.getCode(), MateParticipantStatus.WAITLISTED.getCode())
                .set(MateParticipant::getStatus, MateParticipantStatus.INVALIDATED.getCode()));
    }

    @Override
    @Transactional
    public void joinInvitation(Long userId, Long id, String message) {
        userWriteGuard.checkWritable(userId);
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        if (invitation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能加入自己发布的搭子");
        }
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.MATE_CLOSED);
        }
        if (isExpired(invitation)) {
            throw new BusinessException(ResultCode.MATE_EXPIRED);
        }
        // 拉黑:任一方拉黑不能相互报名
        if (relationshipService.isBlockedEitherWay(userId, invitation.getUserId())) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }

        // 待审/已通过/候补不可重复；拒绝或退出记录保留审计，并允许冷却期后复用
        MateParticipant previous = participantMapper.selectOne(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id)
                .eq(MateParticipant::getUserId, userId)
                .orderByDesc(MateParticipant::getCreatedAt)
                .last("LIMIT 1"));
        if (previous != null && previous.getStatus() != null
                && previous.getStatus() != MateParticipantStatus.REJECTED.getCode()
                && previous.getStatus() != MateParticipantStatus.CANCELLED.getCode()
                && previous.getStatus() != MateParticipantStatus.INVALIDATED.getCode()) {
            throw new BusinessException(ResultCode.ALREADY_JOINED);
        }
        LocalDateTime previousApplyTime = previous == null ? null
                : (previous.getLastAppliedAt() != null ? previous.getLastAppliedAt() : previous.getCreatedAt());
        if (previousApplyTime != null && previousApplyTime.isAfter(LocalDateTime.now().minusHours(24))) {
            throw new BusinessException(ResultCode.ALREADY_JOINED, "申请被拒绝或退出后24小时内不能重复申请");
        }

        // XSS 清洗 + 违禁词拦截
        message = XssUtil.clean(message);
        sensitiveWordFilter.assertClean(message);
        MateParticipant participant = new MateParticipant();
        participant.setInvitationId(id);
        participant.setUserId(userId);
        participant.setMessage(message);
        participant.setApplyCount(1);
        participant.setLastAppliedAt(LocalDateTime.now());
        // 名额已满 → 进入候补;否则待审批
        boolean full = invitation.getCurrentParticipants() >= invitation.getMaxParticipants();
        participant.setStatus(full ? MateParticipantStatus.WAITLISTED.getCode() : MateParticipantStatus.PENDING.getCode());
        if (previous != null) {
            previous.setMessage(message);
            previous.setStatus(participant.getStatus());
            previous.setApplyCount((previous.getApplyCount() == null ? 1 : previous.getApplyCount()) + 1);
            previous.setLastAppliedAt(LocalDateTime.now());
            participantMapper.updateById(previous);
        } else {
            participantMapper.insert(participant);
        }

        // Notify creator
        messageService.notify(userId, invitation.getUserId(), "mate_request", "申请加入你的搭子邀约", id);
    }

    @Override
    @Transactional
    public void leaveInvitation(Long userId, Long id) {
        MateParticipant participant = participantMapper.selectOne(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id)
                .eq(MateParticipant::getUserId, userId));
        if (participant == null) return;

        // 已通过成员退出 → 释放名额并候补补位
        if (participant.getStatus() != null && participant.getStatus() == MateParticipantStatus.ACCEPTED.getCode()) {
            invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                    .eq(MateInvitation::getId, id)
                    .setSql("current_participants = GREATEST(current_participants - 1, 0)"));
            promoteWaitlist(id);
        }
        // 保留审计:置为已退出(不可再次报名同一活动)
        participant.setStatus(MateParticipantStatus.CANCELLED.getCode());
        participantMapper.updateById(participant);
    }

    @Override
    @Transactional
    public void handleJoinRequest(Long userId, Long id, Long participantId, boolean accept) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        if (!invitation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_CREATOR);
        }
        if (invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.MATE_CLOSED);
        }
        if (isExpired(invitation)) {
            throw new BusinessException(ResultCode.MATE_EXPIRED);
        }

        MateParticipant participant = participantMapper.selectById(participantId);
        if (participant == null || !participant.getInvitationId().equals(id)) {
            throw new BusinessException(ResultCode.JOIN_REQUEST_NOT_FOUND);
        }
        // 拉黑:任一方拉黑不能审批
        if (relationshipService.isBlockedEitherWay(userId, participant.getUserId())) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }

        if (accept) {
            // 先原子占用名额，再条件更新报名；任一步失败均回滚，避免“已通过但未占位”。
            int rows = invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                    .eq(MateInvitation::getId, id)
                    .lt(MateInvitation::getCurrentParticipants, invitation.getMaxParticipants())
                    .setSql("current_participants = current_participants + 1"));
            if (rows == 0) {
                throw new BusinessException(ResultCode.MATE_FULL);
            }
            int pRows = participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                    .eq(MateParticipant::getId, participantId)
                    .in(MateParticipant::getStatus,
                            MateParticipantStatus.PENDING.getCode(), MateParticipantStatus.WAITLISTED.getCode())
                    .set(MateParticipant::getStatus, MateParticipantStatus.ACCEPTED.getCode()));
            if (pRows == 0) {
                throw new BusinessException(ResultCode.MATE_APPLY_HANDLED);
            }
            notifyAfterCommit(userId, participant.getUserId(), "mate_accepted", "你的搭子申请已通过", id);
        } else {
            int pRows = participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                    .eq(MateParticipant::getId, participantId)
                    .in(MateParticipant::getStatus,
                            MateParticipantStatus.PENDING.getCode(), MateParticipantStatus.WAITLISTED.getCode())
                    .set(MateParticipant::getStatus, MateParticipantStatus.REJECTED.getCode()));
            if (pRows == 0) {
                throw new BusinessException(ResultCode.MATE_APPLY_HANDLED);
            }
            messageService.notify(userId, participant.getUserId(), "mate_rejected", "你的搭子申请已被拒绝", id);
        }
    }

    @Override
    public PageVO<MateVO> getMyInvitations(Long userId, MateQueryDTO query) {
        Page<MateInvitation> page = new Page<>(query.getPage(), query.getSize());
        Page<MateInvitation> result = invitationMapper.selectPage(page,
                new LambdaQueryWrapper<MateInvitation>()
                        .eq(MateInvitation::getUserId, userId)
                        .orderByDesc(MateInvitation::getCreatedAt));

        Double lat = query.getLatitude() != null ? query.getLatitude().doubleValue() : null;
        Double lng = query.getLongitude() != null ? query.getLongitude().doubleValue() : null;
        List<MateVO> records = result.getRecords().stream()
                .map(m -> toVO(m, userId, lat, lng))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public PageVO<MateVO> getMyJoinedInvitations(Long userId, MateQueryDTO query) {
        LambdaQueryWrapper<MateInvitation> wrapper = new LambdaQueryWrapper<MateInvitation>()
                .inSql(MateInvitation::getId, "SELECT invitation_id FROM mate_participant WHERE user_id = " + userId
                        + " AND status = " + MateParticipantStatus.ACCEPTED.getCode());
        LocalDateTime now = LocalDateTime.now();
        if (query.getStatus() != null && query.getStatus() == 1) {
            wrapper.eq(MateInvitation::getStatus, MateInvitationStatus.ACTIVE.getCode())
                    .gt(MateInvitation::getActivityTime, now);
        } else if (query.getStatus() != null) {
            wrapper.and(w -> w.ne(MateInvitation::getStatus, MateInvitationStatus.ACTIVE.getCode())
                    .or().le(MateInvitation::getActivityTime, now));
        }
        wrapper.orderByDesc(MateInvitation::getActivityTime);
        Page<MateInvitation> result = invitationMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);

        Double lat = query.getLatitude() != null ? query.getLatitude().doubleValue() : null;
        Double lng = query.getLongitude() != null ? query.getLongitude().doubleValue() : null;
        List<MateVO> records = result.getRecords().stream()
                .map(m -> toVO(m, userId, lat, lng))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public PageVO<Map<String, Object>> listMyApplications(Long userId, int page, int size) {
        Page<MateParticipant> pg = new Page<>(page, size);
        Page<MateParticipant> result = participantMapper.selectPage(pg,
                new LambdaQueryWrapper<MateParticipant>()
                        .eq(MateParticipant::getUserId, userId)
                        .orderByDesc(MateParticipant::getCreatedAt));

        Set<Long> invitationIds = result.getRecords().stream()
                .map(MateParticipant::getInvitationId).collect(Collectors.toSet());
        Map<Long, MateInvitation> invMap = new HashMap<>();
        Set<Long> authorIds = new HashSet<>();
        if (!invitationIds.isEmpty()) {
            List<MateInvitation> invs = invitationMapper.selectBatchIds(invitationIds);
            for (MateInvitation i : invs) {
                invMap.put(i.getId(), i);
                authorIds.add(i.getUserId());
            }
        }
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            userMapper.selectBatchIds(authorIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("invitationId", p.getInvitationId());
            map.put("applicationStatus", p.getStatus());
            map.put("message", p.getMessage());
            map.put("applyTime", p.getCreatedAt());
            MateInvitation inv = invMap.get(p.getInvitationId());
            if (inv != null) {
                map.put("title", inv.getTitle());
                map.put("category", inv.getCategory());
                map.put("location", p.getStatus() != null && p.getStatus() == MateParticipantStatus.ACCEPTED.getCode()
                        ? inv.getLocation() : "报名通过后可查看集合地点");
                map.put("activityTime", inv.getActivityTime());
                map.put("invitationStatus", inv.getStatus());
                // 匿名活动不泄露发起人昵称
                map.put("authorNickname", inv.getIsAnonymous() != null && inv.getIsAnonymous() == 1
                        ? "匿名" : nicknameMap.getOrDefault(inv.getUserId(), ""));
            }
            return map;
        }).collect(Collectors.toList());

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public List<Map<String, Object>> listParticipants(Long invitationId, Long currentUserId) {
        MateInvitation invitation = invitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        boolean owner = invitation.getUserId().equals(currentUserId);
        boolean acceptedMember = !owner && participantMapper.selectCount(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, invitationId)
                .eq(MateParticipant::getUserId, currentUserId)
                .eq(MateParticipant::getStatus, MateParticipantStatus.ACCEPTED.getCode())) > 0;
        if (!owner && !acceptedMember) {
            throw new BusinessException(ResultCode.NOT_CREATOR, "仅发起人和已通过成员可查看参与者");
        }
        List<MateParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<MateParticipant>()
                        .eq(MateParticipant::getInvitationId, invitationId)
                        .eq(acceptedMember, MateParticipant::getStatus, MateParticipantStatus.ACCEPTED.getCode())
                        .orderByDesc(MateParticipant::getCreatedAt));
        if (participants.isEmpty()) {
            return List.of();
        }
        Set<Long> uids = participants.stream().map(MateParticipant::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!uids.isEmpty()) {
            userMapper.selectBatchIds(uids).forEach(u -> userMap.put(u.getId(), u));
        }
        return participants.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("participantId", p.getId());
            map.put("userId", p.getUserId());
            if (owner) map.put("message", p.getMessage());
            map.put("status", p.getStatus()); // 0=待审核 1=已通过 2=已拒绝 3=已退出 4=候补
            map.put("statusDesc", MateParticipantStatus.descOf(p.getStatus()));
            map.put("applyTime", p.getCreatedAt());
            User u = userMap.get(p.getUserId());
            if (u != null) {
                map.put("nickname", u.getNickname());
                map.put("avatar", u.getAvatar());
                map.put("school", u.getSchool());
            }
            return map;
        }).collect(Collectors.toList());
    }

    private MateVO toVO(MateInvitation m, Long currentUserId, Double userLat, Double userLng) {
        MateVO vo = new MateVO();
        vo.setId(m.getId());
        // 匿名活动:非发起人/未登录时不返回发起人 ID,避免还原匿名身份(本人仍可见自己)
        boolean anon = m.getIsAnonymous() != null && m.getIsAnonymous() == 1;
        boolean viewerIsOwner = currentUserId != null && m.getUserId().equals(currentUserId);
        MateParticipant mine = currentUserId == null ? null : participantMapper.selectOne(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, m.getId())
                .eq(MateParticipant::getUserId, currentUserId)
                .last("LIMIT 1"));
        boolean viewerAccepted = mine != null && mine.getStatus() != null
                && mine.getStatus() == MateParticipantStatus.ACCEPTED.getCode();
        vo.setUserId(anon && !viewerIsOwner ? null : m.getUserId());
        vo.setCategory(m.getCategory());
        vo.setTitle(m.getTitle());
        vo.setDescription(m.getDescription());
        vo.setActivityTime(m.getActivityTime());
        boolean preciseLocationVisible = viewerIsOwner || viewerAccepted;
        vo.setLocation(preciseLocationVisible ? m.getLocation() : "报名通过后可查看集合地点");
        vo.setLatitude(preciseLocationVisible ? m.getLatitude() : null);
        vo.setLongitude(preciseLocationVisible ? m.getLongitude() : null);
        vo.setMaxParticipants(m.getMaxParticipants());
        vo.setCurrentParticipants(m.getCurrentParticipants());
        vo.setIsAnonymous(m.getIsAnonymous());
        vo.setStatus(m.getStatus());
        vo.setReviewStatus(m.getReviewStatus() != null ? m.getReviewStatus() : 0);
        vo.setReviewReason(m.getReviewReason());
        vo.setCreatedAt(m.getCreatedAt());
        vo.setUpdatedAt(m.getUpdatedAt());
        vo.setIsFull(m.getCurrentParticipants() >= m.getMaxParticipants());
        vo.setIsExpired(m.getActivityTime() != null && m.getActivityTime().isBefore(LocalDateTime.now()));
        vo.setRemainingSlots(Math.max(0, (m.getMaxParticipants() != null ? m.getMaxParticipants() : 0)
                - (m.getCurrentParticipants() != null ? m.getCurrentParticipants() : 0)));

        // Distance
        if (userLat != null && userLng != null && m.getLatitude() != null && m.getLongitude() != null) {
            vo.setDistanceKm(GeoUtils.haversineKm(userLat, userLng,
                    m.getLatitude().doubleValue(), m.getLongitude().doubleValue()));
        }

        // Author:非匿名,或本人查看自己的匿名活动
        if (!anon || viewerIsOwner) {
            vo.setAuthor(userService.getUserProfile(m.getUserId(), currentUserId));
        }

        // 当前用户报名状态(待审核/已通过/候补视为已参与;已退出不算)
        if (currentUserId != null) {
            if (mine != null && mine.getStatus() != null
                    && mine.getStatus() != MateParticipantStatus.CANCELLED.getCode()) {
                vo.setHasJoined(true);
                vo.setMyApplicationStatus(mine.getStatus());
            } else {
                vo.setHasJoined(false);
            }
        }

        return vo;
    }

    private boolean isExpired(MateInvitation invitation) {
        return invitation.getStatus() == MateInvitationStatus.EXPIRED.getCode()
                || (invitation.getActivityTime() != null && invitation.getActivityTime().isBefore(LocalDateTime.now()));
    }

    /** 每分钟收敛过期活动及其未完成申请，通知在事务提交后由消息模块异步处理。 */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireInvitations() {
        List<MateInvitation> expired = invitationMapper.selectList(new LambdaQueryWrapper<MateInvitation>()
                .eq(MateInvitation::getStatus, MateInvitationStatus.ACTIVE.getCode())
                .lt(MateInvitation::getActivityTime, LocalDateTime.now()));
        for (MateInvitation invitation : expired) {
            int rows = invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                    .eq(MateInvitation::getId, invitation.getId())
                    .eq(MateInvitation::getStatus, MateInvitationStatus.ACTIVE.getCode())
                    .set(MateInvitation::getStatus, MateInvitationStatus.EXPIRED.getCode()));
            if (rows == 0) continue;
            notifyMembersAndApplicants(invitation, "该搭子活动已过期，申请已结束");
            participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                    .eq(MateParticipant::getInvitationId, invitation.getId())
                    .in(MateParticipant::getStatus, MateParticipantStatus.PENDING.getCode(), MateParticipantStatus.WAITLISTED.getCode())
                    .set(MateParticipant::getStatus, MateParticipantStatus.INVALIDATED.getCode()));
        }
    }

    /** 定期校正冗余计数，确保 currentParticipants 始终等于发起人加已通过成员数。 */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void reconcileParticipantCounts() {
        List<MateInvitation> invitations = invitationMapper.selectList(new LambdaQueryWrapper<MateInvitation>()
                .in(MateInvitation::getStatus, MateInvitationStatus.ACTIVE.getCode(), MateInvitationStatus.CLOSED.getCode()));
        for (MateInvitation invitation : invitations) {
            long accepted = participantMapper.selectCount(new LambdaQueryWrapper<MateParticipant>()
                    .eq(MateParticipant::getInvitationId, invitation.getId())
                    .eq(MateParticipant::getStatus, MateParticipantStatus.ACCEPTED.getCode()));
            int expected = (int) accepted + 1;
            if (!java.util.Objects.equals(invitation.getCurrentParticipants(), expected)) {
                invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                        .eq(MateInvitation::getId, invitation.getId())
                        .set(MateInvitation::getCurrentParticipants, expected));
            }
        }
    }

    /** 拉黑联动：双方之间待审、候补和已通过关系立即失效，且不向对方暴露拉黑者身份。 */
    @EventListener
    @Transactional
    public void handleUserBlocked(UserBlockedEvent event) {
        invalidateBlockedRelations(event.getUserId(), event.getBlockedUserId());
        invalidateBlockedRelations(event.getBlockedUserId(), event.getUserId());
    }

    private void invalidateBlockedRelations(Long creatorId, Long participantUserId) {
        List<MateInvitation> invitations = invitationMapper.selectList(new LambdaQueryWrapper<MateInvitation>()
                .eq(MateInvitation::getUserId, creatorId));
        if (invitations.isEmpty()) return;
        List<Long> ids = invitations.stream().map(MateInvitation::getId).toList();
        List<MateParticipant> relations = participantMapper.selectList(new LambdaQueryWrapper<MateParticipant>()
                .in(MateParticipant::getInvitationId, ids)
                .eq(MateParticipant::getUserId, participantUserId)
                .in(MateParticipant::getStatus, MateParticipantStatus.PENDING.getCode(),
                        MateParticipantStatus.WAITLISTED.getCode(), MateParticipantStatus.ACCEPTED.getCode()));
        for (MateParticipant relation : relations) {
            int previousStatus = relation.getStatus();
            int rows = participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                    .eq(MateParticipant::getId, relation.getId())
                    .eq(MateParticipant::getStatus, previousStatus)
                    .set(MateParticipant::getStatus, MateParticipantStatus.INVALIDATED.getCode()));
            if (rows == 0) continue;
            if (previousStatus == MateParticipantStatus.ACCEPTED.getCode()) {
                invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                        .eq(MateInvitation::getId, relation.getInvitationId())
                        .gt(MateInvitation::getCurrentParticipants, 1)
                        .setSql("current_participants = current_participants - 1"));
            }
            messageService.notify(null, participantUserId, "mate_relation_ended", "你与该搭子活动的参与关系已结束", relation.getInvitationId());
        }
    }

    /** 候补补位:活动进行中且有名额时,按报名时间把最早的候补提升为已通过 */
    private void promoteWaitlist(Long invitationId) {
        MateInvitation invitation = invitationMapper.selectById(invitationId);
        if (invitation == null || invitation.getStatus() != MateInvitationStatus.ACTIVE.getCode() || isExpired(invitation)) {
            return;
        }
        MateParticipant next = participantMapper.selectOne(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, invitationId)
                .eq(MateParticipant::getStatus, MateParticipantStatus.WAITLISTED.getCode())
                .orderByAsc(MateParticipant::getCreatedAt)
                .last("LIMIT 1"));
        if (next == null) return;
        // 先原子占用名额，再迁移候补状态；任一步失败均抛错并由外层事务回滚。
        int slotRows = invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                .eq(MateInvitation::getId, invitationId)
                .lt(MateInvitation::getCurrentParticipants, invitation.getMaxParticipants())
                .setSql("current_participants = current_participants + 1"));
        if (slotRows == 0) return;
        int pRows = participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                .eq(MateParticipant::getId, next.getId())
                .eq(MateParticipant::getStatus, MateParticipantStatus.WAITLISTED.getCode())
                .set(MateParticipant::getStatus, MateParticipantStatus.ACCEPTED.getCode()));
        if (pRows == 0) {
            throw new BusinessException(ResultCode.MATE_APPLY_HANDLED, "候补状态已变化，请重试");
        }
        messageService.notify(invitation.getUserId(), next.getUserId(), "mate_accepted", "名额有空位，你已补位成功", invitationId);
    }

    /** 通知活动的已通过成员 + 待审批/候补申请人(活动取消/关闭时) */
    private void notifyMembersAndApplicants(MateInvitation invitation, String text) {
        List<MateParticipant> participants = participantMapper.selectList(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, invitation.getId())
                .in(MateParticipant::getStatus,
                        MateParticipantStatus.ACCEPTED.getCode(),
                        MateParticipantStatus.PENDING.getCode(),
                        MateParticipantStatus.WAITLISTED.getCode()));
        for (MateParticipant p : participants) {
            messageService.notify(invitation.getUserId(), p.getUserId(), "mate_cancelled", text, invitation.getId());
        }
    }

    private void notifyAfterCommit(Long fromUserId, Long toUserId, String type, String content, Long referenceId) {
        Runnable notification = () -> messageService.notify(fromUserId, toUserId, type, content, referenceId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { notification.run(); }
            });
        } else {
            notification.run();
        }
    }
}
