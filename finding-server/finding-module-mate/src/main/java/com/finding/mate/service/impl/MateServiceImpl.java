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
import com.finding.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
        LambdaQueryWrapper<MateInvitation> wrapper = new LambdaQueryWrapper<MateInvitation>()
                .eq(MateInvitation::getStatus, 1)
                .eq(MateInvitation::getReviewStatus, 0) // 只显示已发布(审核通过)
                .ge(MateInvitation::getActivityTime, java.time.LocalDateTime.now()); // 只显示未过期的

        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(MateInvitation::getCategory, query.getCategory());
        }
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
        // XSS 清洗 + 拦截/送审分类
        dto.setTitle(XssUtil.clean(dto.getTitle()));
        dto.setDescription(XssUtil.clean(dto.getDescription()));
        ReviewResult review = sensitiveWordFilter.classifyReview(dto.getTitle(), dto.getDescription());
        if (review.hasBlocking()) {
            String joined = review.blocking().stream().map(w -> "「" + w + "」").collect(Collectors.joining());
            throw new BusinessException(ResultCode.CONTENT_BLOCKED, "内容包含违禁词:" + joined);
        }
        MateInvitation invitation = new MateInvitation();
        invitation.setUserId(userId);
        invitation.setCategory(dto.getCategory());
        invitation.setTitle(dto.getTitle());
        invitation.setDescription(dto.getDescription());
        invitation.setActivityTime(dto.getActivityTime());
        invitation.setLocation(dto.getLocation());
        invitation.setLatitude(dto.getLatitude());
        invitation.setLongitude(dto.getLongitude());
        invitation.setMaxParticipants(dto.getMaxParticipants());
        invitation.setCurrentParticipants(1);
        invitation.setIsAnonymous(dto.getIsAnonymous());
        invitation.setStatus(1);
        invitation.setReviewStatus(review.hasReview() ? 1 : 0);
        invitationMapper.insert(invitation);
        return toVO(invitation, userId, null, null);
    }

    @Override
    public void updateInvitation(Long userId, Long id, MateCreateDTO dto) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        if (!invitation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_CREATOR);
        }
        invitation.setCategory(dto.getCategory());
        invitation.setTitle(dto.getTitle());
        invitation.setDescription(dto.getDescription());
        invitation.setActivityTime(dto.getActivityTime());
        invitation.setLocation(dto.getLocation());
        invitation.setMaxParticipants(dto.getMaxParticipants());
        dto.setTitle(XssUtil.clean(dto.getTitle()));
        dto.setDescription(XssUtil.clean(dto.getDescription()));
        sensitiveWordFilter.assertClean(dto.getTitle(), dto.getDescription());
        invitationMapper.updateById(invitation);
    }

    @Override
    @Transactional
    public void cancelInvitation(Long userId, Long id) {
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

        // 已有报名记录(任意状态)不可再报
        long count = participantMapper.selectCount(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id)
                .eq(MateParticipant::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_JOINED);
        }

        // XSS 清洗 + 违禁词拦截
        message = XssUtil.clean(message);
        sensitiveWordFilter.assertClean(message);
        MateParticipant participant = new MateParticipant();
        participant.setInvitationId(id);
        participant.setUserId(userId);
        participant.setMessage(message);
        // 名额已满 → 进入候补;否则待审批
        boolean full = invitation.getCurrentParticipants() >= invitation.getMaxParticipants();
        participant.setStatus(full ? MateParticipantStatus.WAITLISTED.getCode() : MateParticipantStatus.PENDING.getCode());
        participantMapper.insert(participant);

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
            // 条件更新报名:仅待审批/候补可被通过,防并发重复审批
            int pRows = participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                    .eq(MateParticipant::getId, participantId)
                    .in(MateParticipant::getStatus,
                            MateParticipantStatus.PENDING.getCode(), MateParticipantStatus.WAITLISTED.getCode())
                    .set(MateParticipant::getStatus, MateParticipantStatus.ACCEPTED.getCode()));
            if (pRows == 0) {
                throw new BusinessException(ResultCode.MATE_APPLY_HANDLED);
            }
            // 原子占用名额:current < max 才自增,并发下防超卖
            int rows = invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                    .eq(MateInvitation::getId, id)
                    .lt(MateInvitation::getCurrentParticipants, invitation.getMaxParticipants())
                    .setSql("current_participants = current_participants + 1"));
            if (rows == 0) {
                throw new BusinessException(ResultCode.MATE_FULL);
            }
            messageService.notify(userId, participant.getUserId(), "mate_accepted", "你的搭子申请已通过", id);
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
        // 查到所有加入的搭子
        List<MateParticipant> allParticipants = participantMapper.selectList(
                new LambdaQueryWrapper<MateParticipant>()
                        .eq(MateParticipant::getUserId, userId)
                        .eq(MateParticipant::getStatus, 1));

        if (allParticipants.isEmpty()) {
            return PageVO.of(List.of(), 0L, query.getPage(), query.getSize());
        }

        List<Long> invitationIds = allParticipants.stream()
                .map(MateParticipant::getInvitationId).distinct().toList();

        // 查搭子详情
        List<MateInvitation> allInvitations = invitationMapper.selectBatchIds(invitationIds);
        LocalDateTime now = LocalDateTime.now();

        // 按状态过滤
        List<MateInvitation> filtered;
        if (query.getStatus() == null) {
            // 全部
            filtered = new ArrayList<>(allInvitations);
        } else if (query.getStatus() == 1) {
            // 进行中：status=1 且 activityTime 未过
            filtered = allInvitations.stream()
                    .filter(m -> m.getStatus() == 1 &&
                            (m.getActivityTime() == null || m.getActivityTime().isAfter(now)))
                    .collect(Collectors.toList());
        } else {
            // 已结束：status=2 或 activityTime 已过
            filtered = allInvitations.stream()
                    .filter(m -> m.getStatus() == 2 ||
                            (m.getActivityTime() != null && !m.getActivityTime().isAfter(now)))
                    .collect(Collectors.toList());
        }

        // 按活动时间排序（最近的在前）
        filtered.sort((a, b) -> {
            LocalDateTime ta = a.getActivityTime() != null ? a.getActivityTime() : a.getCreatedAt();
            LocalDateTime tb = b.getActivityTime() != null ? b.getActivityTime() : b.getCreatedAt();
            return tb.compareTo(ta); // 降序
        });

        long total = filtered.size();
        int from = Math.min((query.getPage() - 1) * query.getSize(), filtered.size());
        int to = Math.min(from + query.getSize(), filtered.size());
        List<MateInvitation> paged = filtered.subList(from, to);

        Double lat = query.getLatitude() != null ? query.getLatitude().doubleValue() : null;
        Double lng = query.getLongitude() != null ? query.getLongitude().doubleValue() : null;
        List<MateVO> records = paged.stream()
                .map(m -> toVO(m, userId, lat, lng))
                .collect(Collectors.toList());
        return PageVO.of(records, total, query.getPage(), query.getSize());
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
                map.put("location", inv.getLocation());
                map.put("activityTime", inv.getActivityTime());
                map.put("invitationStatus", inv.getStatus());
                map.put("authorNickname", nicknameMap.getOrDefault(inv.getUserId(), ""));
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
        if (!invitation.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.NOT_CREATOR);
        }
        List<MateParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<MateParticipant>()
                        .eq(MateParticipant::getInvitationId, invitationId)
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
            map.put("message", p.getMessage());
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
        vo.setUserId(m.getUserId());
        vo.setCategory(m.getCategory());
        vo.setTitle(m.getTitle());
        vo.setDescription(m.getDescription());
        vo.setActivityTime(m.getActivityTime());
        vo.setLocation(m.getLocation());
        vo.setLatitude(m.getLatitude());
        vo.setLongitude(m.getLongitude());
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

        // Author (mask if anonymous)
        if (m.getIsAnonymous() != 1) {
            vo.setAuthor(userService.getUserProfile(m.getUserId(), currentUserId));
        }

        // 当前用户报名状态(待审核/已通过/候补视为已参与;已退出不算)
        if (currentUserId != null) {
            MateParticipant mine = participantMapper.selectOne(new LambdaQueryWrapper<MateParticipant>()
                    .eq(MateParticipant::getInvitationId, m.getId())
                    .eq(MateParticipant::getUserId, currentUserId)
                    .last("LIMIT 1"));
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
        return invitation.getActivityTime() != null && invitation.getActivityTime().isBefore(LocalDateTime.now());
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
        // 条件更新防并发:仅当前仍在候补才可提升
        int pRows = participantMapper.update(null, new LambdaUpdateWrapper<MateParticipant>()
                .eq(MateParticipant::getId, next.getId())
                .eq(MateParticipant::getStatus, MateParticipantStatus.WAITLISTED.getCode())
                .set(MateParticipant::getStatus, MateParticipantStatus.ACCEPTED.getCode()));
        if (pRows == 0) return;
        invitationMapper.update(null, new LambdaUpdateWrapper<MateInvitation>()
                .eq(MateInvitation::getId, invitationId)
                .lt(MateInvitation::getCurrentParticipants, invitation.getMaxParticipants())
                .setSql("current_participants = current_participants + 1"));
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
}
