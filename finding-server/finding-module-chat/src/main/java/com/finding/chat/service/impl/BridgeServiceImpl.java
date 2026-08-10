package com.finding.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.user.common.VerificationGuard;
import com.finding.chat.entity.ChatApply;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.Room;
import com.finding.user.entity.User;
import com.finding.user.entity.UserFollow;
import com.finding.user.entity.UserSettings;
import com.finding.chat.mapper.ChatApplyMapper;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RoomMapper;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.service.UserRelationshipService;
import com.finding.chat.service.BridgeService;
import com.finding.chat.service.ChatService;
import com.finding.message.service.MessageService;
import com.finding.common.GeoUtils;
import com.finding.chat.vo.ChatApplyVO;
import com.finding.chat.vo.HomeFeedVO;
import com.finding.common.PageVO;
import com.finding.common.word.SensitiveWordFilter;
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
    private final MessageService messageService;
    private final RoomMapper roomMapper;
    private final PrivateChatMapper privateChatMapper;
    private final ContactMapper contactMapper;
    private final ChatService chatService;
    private final VerificationGuard verificationGuard;
    private final UserSettingsMapper userSettingsMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final UserRelationshipService relationshipService;

    @Override
    public PageVO<HomeFeedVO> getRecommendFeed(Long userId, Double lat, Double lng, int page, int size) {
        Set<Long> excludeIds = new HashSet<>();
        User currentUser = null;
        if (userId != null) {
            excludeIds.add(userId);
            currentUser = userMapper.selectById(userId);

            // 排除已申请过的
            List<ChatApply> sentApplies = chatApplyMapper.selectList(
                    new LambdaQueryWrapper<ChatApply>().eq(ChatApply::getFromUserId, userId));
            sentApplies.forEach(a -> excludeIds.add(a.getToUserId()));

            // 排除已关注的
            List<UserFollow> follows = followMapper.selectList(
                    new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));
            follows.forEach(f -> excludeIds.add(f.getFolloweeId()));

            // 排除与当前用户双向拉黑的用户
            excludeIds.addAll(relationshipService.blockedUserIds(userId));
        }

        // 排除关闭"允许被搜索"的用户(关闭搜索同时不出现在相亲推荐)
        List<Long> hiddenIds = userSettingsMapper.selectList(
                        new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getSearchable, 0))
                .stream().map(UserSettings::getUserId).toList();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .notIn(!hiddenIds.isEmpty(), User::getId, hiddenIds);
        if (!excludeIds.isEmpty()) {
            wrapper.notIn(User::getId, excludeIds);
        }

        // ── 候选全量过滤 → 打分 → 稳定排序 → 分页 ──
        // 全量候选(校园规模可控)在内存中按得分稳定排序,保证翻页不跳不重、total 准确
        List<User> candidates = userMapper.selectList(wrapper);
        final User me = currentUser;
        candidates.sort((a, b) -> {
            int cmp = Integer.compare(matchScore(b, me), matchScore(a, me)); // 得分降序
            return cmp != 0 ? cmp : Long.compare(b.getId(), a.getId());     // 同分按 id 降序(稳定)
        });

        int total = candidates.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<User> paged = candidates.subList(from, to);

        List<HomeFeedVO> records = paged.stream()
                .map(u -> toFeedVO(u, lat, lng, userId))
                .collect(Collectors.toList());
        return PageVO.of(records, (long) total, page, size);
    }

    /**
     * 相亲匹配评分：异性 +20、同校 +15、同城 +8、已认证 +5、
     * 最近活跃 +3、兴趣关键词匹配 +2/词、有头像 +2。
     */
    private int matchScore(User candidate, User me) {
        int score = 0;
        if (me == null) return score;

        // 异性优先（相亲核心）
        if (me.getGender() != null && candidate.getGender() != null
                && me.getGender() > 0 && candidate.getGender() > 0
                && !me.getGender().equals(candidate.getGender())) {
            score += 20;
        }

        // 同校（大学生相亲最重要）
        if (me.getSchool() != null && me.getSchool().equals(candidate.getSchool())) {
            score += 15;
        }

        // 同城
        if (me.getCity() != null && me.getCity().equals(candidate.getCity())) {
            score += 3;
        }

        // 已认证
        if (candidate.getRealNameVerified() != null && candidate.getRealNameVerified() == 2) {
            score += 5;
        }

        // 最近 24h 活跃
        if (candidate.getLastLoginAt() != null
                && candidate.getLastLoginAt().isAfter(LocalDateTime.now().minusHours(24))) {
            score += 3;
        }

        // 有头像（更真诚）
        if (candidate.getAvatar() != null && !candidate.getAvatar().isEmpty()) {
            score += 2;
        }

        // 兴趣标签 / 个性签名关键词匹配
        if (me.getSignature() != null && candidate.getSignature() != null) {
            String[] myWords = me.getSignature().split("[，。！？,.!?\\s]+");
            String theirSig = candidate.getSignature();
            for (String w : myWords) {
                if (w.length() >= 2 && theirSig.contains(w)) {
                    score += 2;
                }
            }
        }

        return score;
    }

    @Override
    @Transactional
    public void applyChat(Long fromUserId, Long toUserId, String remark) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能给自己发送申请");
        }
        // Check real-name verification
        verificationGuard.checkVerified(fromUserId);

        // 拉黑拦截:任一方拉黑对方都无法发送申请
        if (relationshipService.isBlockedEitherWay(fromUserId, toUserId)) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }

        // Check if target user exists
        User targetUser = userMapper.selectById(toUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 加好友方式:2=不允许申请直接拒绝;0=所有人可申请(自动通过);1=需验证(默认)
        UserSettings targetSettings = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, toUserId));
        int friendMode = targetSettings != null && targetSettings.getFriendAddMode() != null
                ? targetSettings.getFriendAddMode() : 1;
        if (friendMode == 2) {
            throw new BusinessException(ResultCode.CONTACT_PERMISSION_DENIED);
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
        // 申请备注含违禁词直接拒绝
        sensitiveWordFilter.assertClean(remark);
        chatApplyMapper.insert(apply);

        if (friendMode == 0) {
            // 所有人可申请 → 自动通过并建立会话
            approveApply(apply);
        } else {
            // 需验证(默认) → 通知对方审核
            User fromUser = userMapper.selectById(fromUserId);
            messageService.notify(fromUserId, toUserId, "chat_apply",
                    (fromUser != null ? fromUser.getNickname() : "有人") + "向你发送了聊天申请", apply.getId());
        }

        log.info("Chat apply: user {} → user {}, applyId={}, friendMode={}", fromUserId, toUserId, apply.getId(), friendMode);
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
    public long countPendingReceived(Long userId) {
        return chatApplyMapper.selectCount(new LambdaQueryWrapper<ChatApply>()
                .eq(ChatApply::getToUserId, userId)
                .eq(ChatApply::getStatus, 0));
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

        if (status == 1) {
            // 通过:建会话 + 系统消息 + 通知申请人
            approveApply(apply);
        } else {
            User handler = userMapper.selectById(userId);
            messageService.notify(userId, apply.getFromUserId(), "chat_rejected",
                    "你的聊天申请已拒绝" + (handler != null ? "（" + handler.getNickname() + "）" : ""), applyId);
        }
    }

    // ── Private helpers ──

    /** 通过聊天申请:置状态 + 建会话/系统消息 + 通知申请人 */
    private void approveApply(ChatApply apply) {
        apply.setStatus(1);
        apply.setHandleTime(LocalDateTime.now());
        chatApplyMapper.updateById(apply);

        try {
            var convVO = chatService.getOrCreateConversation(apply.getToUserId(), apply.getFromUserId());
            Long roomId = convVO.getRoomId();
            log.info("Room created for applyId={}: user {} ↔ user {}, roomId={}", apply.getId(), apply.getToUserId(), apply.getFromUserId(), roomId);

            PrivateChat systemMsg = new PrivateChat();
            systemMsg.setConversationId(roomId); // 兼容旧字段
            systemMsg.setRoomId(roomId);
            systemMsg.setFromUserId(apply.getToUserId());
            systemMsg.setToUserId(apply.getFromUserId());
            systemMsg.setContent("已同意申请，可以开始聊天了");
            systemMsg.setMessageType("text");
            systemMsg.setIsRead(0);
            privateChatMapper.insert(systemMsg);

            Room room = roomMapper.selectById(roomId);
            if (room != null) {
                room.setActiveTime(LocalDateTime.now());
                room.setLastMsgId(systemMsg.getId());
                roomMapper.updateById(room);
            }

            updateContact(apply.getToUserId(), roomId, systemMsg.getId());
            updateContact(apply.getFromUserId(), roomId, systemMsg.getId());
        } catch (Exception e) {
            log.error("Failed to create conversation for applyId={}", apply.getId(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建会话失败，请重试");
        }

        // 互相同意 → 自动互相关注
        insertFollowIfAbsent(apply.getFromUserId(), apply.getToUserId());
        insertFollowIfAbsent(apply.getToUserId(), apply.getFromUserId());

        User handler = userMapper.selectById(apply.getToUserId());
        messageService.notify(apply.getToUserId(), apply.getFromUserId(), "chat_approved",
                "你的聊天申请已通过" + (handler != null ? "（" + handler.getNickname() + "）" : ""), apply.getId());
    }

    /** 若未关注则插入一条关注关系(自动互关用);双向拉黑时跳过 */
    private void insertFollowIfAbsent(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) return;
        if (relationshipService.isBlockedEitherWay(followerId, followeeId)) return;
        Long count = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));
        if (count == 0) {
            UserFollow f = new UserFollow();
            f.setFollowerId(followerId);
            f.setFolloweeId(followeeId);
            followMapper.insert(f);
        }
    }

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


    private HomeFeedVO toFeedVO(User user, Double lat, Double lng, Long currentUserId) {
        HomeFeedVO vo = new HomeFeedVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        // 资料可见性投影:不可查看详细资料时只返回公开字段(学校公开,性别/签名/城市隐藏)
        boolean detailed = relationshipService.canViewDetailedProfile(currentUserId, user.getId());
        vo.setGender(detailed ? user.getGender() : null);
        vo.setSchool(user.getSchool());
        vo.setSignature(detailed ? user.getSignature() : null);
        vo.setCity(detailed ? user.getCity() : null);
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
