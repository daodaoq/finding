package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.dto.MateCreateDTO;
import com.finding.dto.MateQueryDTO;
import com.finding.entity.*;
import com.finding.mapper.*;
import com.finding.service.MateService;
import com.finding.service.UserService;
import com.finding.utils.GeoUtils;
import com.finding.vo.MateVO;
import com.finding.vo.PageVO;
import com.finding.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MateServiceImpl implements MateService {

    private final MateInvitationMapper invitationMapper;
    private final MateParticipantMapper participantMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    public PageVO<MateVO> listInvitations(MateQueryDTO query, Long currentUserId) {
        LambdaQueryWrapper<MateInvitation> wrapper = new LambdaQueryWrapper<MateInvitation>()
                .eq(MateInvitation::getStatus, 1)
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
        return toVO(invitation, currentUserId, null, null);
    }

    @Override
    @Transactional
    public MateVO createInvitation(Long userId, MateCreateDTO dto) {
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
        invitationMapper.updateById(invitation);
    }

    @Override
    public void cancelInvitation(Long userId, Long id) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        if (!invitation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_CREATOR);
        }
        invitation.setStatus(0);
        invitationMapper.updateById(invitation);
    }

    @Override
    @Transactional
    public void joinInvitation(Long userId, Long id, String message) {
        MateInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null || invitation.getStatus() != 1) {
            throw new BusinessException(ResultCode.MATE_NOT_FOUND);
        }
        if (invitation.getCurrentParticipants() >= invitation.getMaxParticipants()) {
            throw new BusinessException(ResultCode.MATE_FULL);
        }

        long count = participantMapper.selectCount(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id)
                .eq(MateParticipant::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_JOINED);
        }

        MateParticipant participant = new MateParticipant();
        participant.setInvitationId(id);
        participant.setUserId(userId);
        participant.setMessage(message);
        participant.setStatus(0); // pending
        participantMapper.insert(participant);

        // Notify creator
        Message msg = new Message();
        msg.setFromUserId(userId);
        msg.setToUserId(invitation.getUserId());
        msg.setType("mate_request");
        msg.setContent("申请加入你的搭子邀约");
        msg.setRelatedId(id);
        messageMapper.insert(msg);
    }

    @Override
    public void leaveInvitation(Long userId, Long id) {
        participantMapper.delete(new LambdaQueryWrapper<MateParticipant>()
                .eq(MateParticipant::getInvitationId, id)
                .eq(MateParticipant::getUserId, userId));
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

        MateParticipant participant = participantMapper.selectById(participantId);
        if (participant == null || !participant.getInvitationId().equals(id)) {
            throw new BusinessException(ResultCode.JOIN_REQUEST_NOT_FOUND);
        }

        participant.setStatus(accept ? 1 : 2);
        participantMapper.updateById(participant);

        if (accept) {
            invitation.setCurrentParticipants(invitation.getCurrentParticipants() + 1);
            invitationMapper.updateById(invitation);

            Message msg = new Message();
            msg.setFromUserId(userId);
            msg.setToUserId(participant.getUserId());
            msg.setType("mate_accepted");
            msg.setContent("你的搭子申请已通过");
            msg.setRelatedId(id);
            messageMapper.insert(msg);
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
        vo.setCreatedAt(m.getCreatedAt());
        vo.setUpdatedAt(m.getUpdatedAt());
        vo.setIsFull(m.getCurrentParticipants() >= m.getMaxParticipants());

        // Distance
        if (userLat != null && userLng != null && m.getLatitude() != null && m.getLongitude() != null) {
            vo.setDistanceKm(GeoUtils.haversineKm(userLat, userLng,
                    m.getLatitude().doubleValue(), m.getLongitude().doubleValue()));
        }

        // Author (mask if anonymous)
        if (m.getIsAnonymous() != 1) {
            vo.setAuthor(userService.getUserProfile(m.getUserId(), currentUserId));
        }

        // Has current user joined
        if (currentUserId != null) {
            vo.setHasJoined(participantMapper.selectCount(new LambdaQueryWrapper<MateParticipant>()
                    .eq(MateParticipant::getInvitationId, m.getId())
                    .eq(MateParticipant::getUserId, currentUserId)) > 0);
        }

        return vo;
    }
}
