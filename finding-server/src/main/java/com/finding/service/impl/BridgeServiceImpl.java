package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.VerificationGuard;
import com.finding.entity.ChatApply;
import com.finding.entity.Contact;
import com.finding.entity.Message;
import com.finding.entity.PrivateChat;
import com.finding.entity.Room;
import com.finding.entity.User;
import com.finding.entity.UserFollow;
import com.finding.mapper.ChatApplyMapper;
import com.finding.mapper.ContactMapper;
import com.finding.mapper.MessageMapper;
import com.finding.mapper.PrivateChatMapper;
import com.finding.mapper.RoomMapper;
import com.finding.mapper.UserFollowMapper;
import com.finding.mapper.UserMapper;
import com.finding.service.BridgeService;
import com.finding.service.ChatService;
import com.finding.utils.GeoUtils;
import com.finding.vo.ChatApplyVO;
import com.finding.vo.HomeFeedVO;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BridgeServiceImpl implements BridgeService {

    private final UserMapper userMapper;
    private final ChatApplyMapper chatApplyMapper;
    private final UserFollowMapper followMapper;
    private final MessageMapper messageMapper;
    private final RoomMapper roomMapper;
    private final PrivateChatMapper privateChatMapper;
    private final ContactMapper contactMapper;
    private final ChatService chatService;
    private final VerificationGuard verificationGuard;

    @Override
    public PageVO<HomeFeedVO> getRecommendFeed(Long userId, Double lat, Double lng, int page, int size) {
        // Collect IDs to exclude: self, already applied, already followed
        Set<Long> excludeIds = new HashSet<>();
        if (userId != null) {
            excludeIds.add(userId);

            // Exclude users already applied to
            List<ChatApply> sentApplies = chatApplyMapper.selectList(
                    new LambdaQueryWrapper<ChatApply>().eq(ChatApply::getFromUserId, userId));
            sentApplies.forEach(a -> excludeIds.add(a.getToUserId()));

            // Exclude already followed users
            List<UserFollow> follows = followMapper.selectList(
                    new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));
            follows.forEach(f -> excludeIds.add(f.getFolloweeId()));
        }

        // Build query — prioritize verified + active users
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1);
        if (!excludeIds.isEmpty()) {
            wrapper.notIn(User::getId, excludeIds);
        }

        // Sorting: prioritize verified users, then by last login
        wrapper.orderByDesc(User::getRealNameVerified)
               .orderByDesc(User::getLastLoginAt);

        Page<User> pg = new Page<>(page, size);
        Page<User> result = userMapper.selectPage(pg, wrapper);

        // If user has school, sort same-school users to the top (in-memory reorder)
        final String currentUserSchool;
        final String currentUserCity;
        if (userId != null) {
            User currentUser = userMapper.selectById(userId);
            if (currentUser != null) {
                currentUserSchool = currentUser.getSchool();
                currentUserCity = currentUser.getCity();
            } else {
                currentUserSchool = null;
                currentUserCity = null;
            }
        } else {
            currentUserSchool = null;
            currentUserCity = null;
        }

        List<User> sortedRecords = new ArrayList<>(result.getRecords());
        if (currentUserSchool != null || currentUserCity != null) {
            final String school = currentUserSchool;
            final String city = currentUserCity;
            sortedRecords.sort((a, b) -> {
                int scoreA = getPriorityScore(a, school, city);
                int scoreB = getPriorityScore(b, school, city);
                return Integer.compare(scoreB, scoreA); // higher score first
            });
        }

        List<HomeFeedVO> records = sortedRecords.stream()
                .map(u -> toFeedVO(u, lat, lng, userId))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void applyChat(Long fromUserId, Long toUserId, String remark) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能给自己发送申请");
        }
        // Check real-name verification
        verificationGuard.checkVerified(fromUserId);

        // Check if target user exists
        User targetUser = userMapper.selectById(toUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // Check duplicate application
        Long count = chatApplyMapper.selectCount(new LambdaQueryWrapper<ChatApply>()
                .eq(ChatApply::getFromUserId, fromUserId)
                .eq(ChatApply::getToUserId, toUserId));
        if (count > 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_SENT);
        }

        // Insert application
        ChatApply apply = new ChatApply();
        apply.setFromUserId(fromUserId);
        apply.setToUserId(toUserId);
        apply.setStatus(0); // pending
        apply.setRemark(remark);
        apply.setApplyTime(LocalDateTime.now());
        chatApplyMapper.insert(apply);

        // Send notification to target user
        User fromUser = userMapper.selectById(fromUserId);
        Message msg = new Message();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setType("chat_apply");
        msg.setContent((fromUser != null ? fromUser.getNickname() : "有人") + "向你发送了聊天申请");
        msg.setRelatedId(apply.getId());
        messageMapper.insert(msg);

        log.info("Chat apply sent: user {} → user {}, applyId={}", fromUserId, toUserId, apply.getId());
    }

    @Override
    public PageVO<ChatApplyVO> getSentApplies(Long userId, int page, int size) {
        Page<ChatApply> pg = new Page<>(page, size);
        Page<ChatApply> result = chatApplyMapper.selectPage(pg,
                new LambdaQueryWrapper<ChatApply>()
                        .eq(ChatApply::getFromUserId, userId)
                        .orderByDesc(ChatApply::getApplyTime));

        List<ChatApplyVO> records = result.getRecords().stream()
                .map(a -> toSentApplyVO(a))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public PageVO<ChatApplyVO> getReceivedApplies(Long userId, int page, int size) {
        Page<ChatApply> pg = new Page<>(page, size);
        Page<ChatApply> result = chatApplyMapper.selectPage(pg,
                new LambdaQueryWrapper<ChatApply>()
                        .eq(ChatApply::getToUserId, userId)
                        .orderByDesc(ChatApply::getApplyTime));

        List<ChatApplyVO> records = result.getRecords().stream()
                .map(a -> toReceivedApplyVO(a))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void handleApply(Long userId, Long applyId, Integer status) {
        ChatApply apply = chatApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.CHAT_APPLY_NOT_FOUND);
        }
        // Only the receiver can handle
        if (!apply.getToUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权处理该申请");
        }
        // Only pending applications can be handled
        if (apply.getStatus() != 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_HANDLED);
        }

        apply.setStatus(status);
        apply.setHandleTime(LocalDateTime.now());
        chatApplyMapper.updateById(apply);

        String actionText = status == 1 ? "已通过" : "已拒绝";

        if (status == 1) {
            // Approved — auto-create Room + send system message
            try {
                var convVO = chatService.getOrCreateConversation(userId, apply.getFromUserId());
                Long roomId = convVO.getRoomId();
                log.info("Room created for applyId={}: user {} ↔ user {}, roomId={}", applyId, userId, apply.getFromUserId(), roomId);

                // Insert system message via room_id
                PrivateChat systemMsg = new PrivateChat();
                systemMsg.setConversationId(roomId); // 兼容旧字段
                systemMsg.setRoomId(roomId);
                systemMsg.setFromUserId(userId);
                systemMsg.setToUserId(apply.getFromUserId());
                systemMsg.setContent("已同意申请，可以开始聊天了");
                systemMsg.setMessageType("text");
                systemMsg.setIsRead(0);
                privateChatMapper.insert(systemMsg);

                // Update room active_time
                Room room = roomMapper.selectById(roomId);
                if (room != null) {
                    room.setActiveTime(LocalDateTime.now());
                    room.setLastMsgId(systemMsg.getId());
                    roomMapper.updateById(room);
                }

                // Update both contacts
                updateContact(userId, roomId, systemMsg.getId());
                updateContact(apply.getFromUserId(), roomId, systemMsg.getId());
            } catch (Exception e) {
                log.error("Failed to create conversation for applyId={}", applyId, e);
            }
        }

        // Notify applicant
        User handler = userMapper.selectById(userId);
        Message msg = new Message();
        msg.setFromUserId(userId);
        msg.setToUserId(apply.getFromUserId());
        msg.setType(status == 1 ? "chat_approved" : "chat_rejected");
        msg.setContent("你的聊天申请" + actionText + (handler != null ? "（" + handler.getNickname() + "）" : ""));
        msg.setRelatedId(applyId);
        messageMapper.insert(msg);
    }

    // ── Private helpers ──

    /** 更新/创建 contact 记录 */
    private void updateContact(Long uid, Long roomId, Long msgId) {
        Contact contact = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, uid)
                        .eq(Contact::getRoomId, roomId));
        if (contact == null) {
            contact = new Contact();
            contact.setUid(uid);
            contact.setRoomId(roomId);
            contact.setActiveTime(LocalDateTime.now());
            contact.setLastMsgId(msgId);
            contactMapper.insert(contact);
        } else {
            contact.setActiveTime(LocalDateTime.now());
            contact.setLastMsgId(msgId);
            contactMapper.updateById(contact);
        }
    }

    /** Priority score for sorting: same school=10, same city=5, recently active=1 */
    private int getPriorityScore(User user, String currentSchool, String currentCity) {
        int score = 0;
        if (currentSchool != null && currentSchool.equals(user.getSchool())) {
            score += 10;
        }
        if (currentCity != null && currentCity.equals(user.getCity())) {
            score += 5;
        }
        // Recent activity bonus (within 24h)
        if (user.getLastLoginAt() != null &&
                user.getLastLoginAt().isAfter(LocalDateTime.now().minusHours(24))) {
            score += 1;
        }
        return score;
    }

    private HomeFeedVO toFeedVO(User user, Double lat, Double lng, Long currentUserId) {
        HomeFeedVO vo = new HomeFeedVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setSchool(user.getSchool());
        vo.setSignature(user.getSignature());
        vo.setCity(user.getCity());
        vo.setLastLoginAt(user.getLastLoginAt());

        // Distance calculation
        if (lat != null && lng != null && user.getLatitude() != null && user.getLongitude() != null) {
            vo.setDistanceKm(GeoUtils.haversineKm(lat, lng,
                    user.getLatitude().doubleValue(), user.getLongitude().doubleValue()));
        }

        // Check if current user has already sent application
        if (currentUserId != null) {
            vo.setIsLiked(chatApplyMapper.selectCount(new LambdaQueryWrapper<ChatApply>()
                    .eq(ChatApply::getFromUserId, currentUserId)
                    .eq(ChatApply::getToUserId, user.getId())) > 0);
        }

        return vo;
    }

    private ChatApplyVO toSentApplyVO(ChatApply apply) {
        ChatApplyVO vo = new ChatApplyVO();
        vo.setId(apply.getId());
        vo.setFromUserId(apply.getFromUserId());
        vo.setToUserId(apply.getToUserId());
        vo.setStatus(apply.getStatus());
        vo.setStatusDesc(getStatusDesc(apply.getStatus()));
        vo.setRemark(apply.getRemark());
        vo.setApplyTime(apply.getApplyTime());
        vo.setHandleTime(apply.getHandleTime());

        // Load target user info
        User target = userMapper.selectById(apply.getToUserId());
        if (target != null) {
            vo.setToUserNickname(target.getNickname());
            vo.setToUserAvatar(target.getAvatar());
        }
        return vo;
    }

    private ChatApplyVO toReceivedApplyVO(ChatApply apply) {
        ChatApplyVO vo = new ChatApplyVO();
        vo.setId(apply.getId());
        vo.setFromUserId(apply.getFromUserId());
        vo.setToUserId(apply.getToUserId());
        vo.setStatus(apply.getStatus());
        vo.setStatusDesc(getStatusDesc(apply.getStatus()));
        vo.setRemark(apply.getRemark());
        vo.setApplyTime(apply.getApplyTime());
        vo.setHandleTime(apply.getHandleTime());

        // Load applicant user info
        User from = userMapper.selectById(apply.getFromUserId());
        if (from != null) {
            vo.setFromUserNickname(from.getNickname());
            vo.setFromUserAvatar(from.getAvatar());
        }
        return vo;
    }

    private String getStatusDesc(Integer status) {
        return switch (status) {
            case 0 -> "待通过";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };
    }
}
