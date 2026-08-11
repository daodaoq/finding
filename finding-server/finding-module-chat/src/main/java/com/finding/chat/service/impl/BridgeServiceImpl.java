package com.finding.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.user.common.VerificationGuard;
import com.finding.chat.constant.ChatApplyStatus;
import com.finding.chat.config.MatchScoreWeights;
import com.finding.chat.entity.ChatApply;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.RecommendEvent;
import com.finding.chat.entity.RecommendExclude;
import com.finding.chat.entity.Room;
import com.finding.chat.entity.UserMatchPreference;
import com.finding.user.entity.User;
import com.finding.user.entity.UserFollow;
import com.finding.user.entity.UserSettings;
import com.finding.chat.mapper.ChatApplyMapper;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RecommendEventMapper;
import com.finding.chat.mapper.RecommendExcludeMapper;
import com.finding.chat.mapper.RoomMapper;
import com.finding.chat.mapper.UserMatchPreferenceMapper;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserWriteGuard;
import com.finding.chat.service.BridgeService;
import com.finding.chat.service.ChatService;
import com.finding.message.service.MessageService;
import com.finding.framework.util.InMemoryRateLimiter;
import com.finding.common.GeoUtils;
import com.finding.chat.vo.ChatApplyVO;
import com.finding.chat.vo.HomeFeedVO;
import com.finding.common.PageVO;
import com.finding.common.word.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BridgeServiceImpl implements BridgeService {

    /** 申请被拒绝/撤回后的冷却期(天内不能重发) */
    private static final int APPLY_COOLDOWN_DAYS = 7;
    /** 待处理申请超期惰性过期的天数 */
    private static final int APPLY_EXPIRE_DAYS = 7;

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
    private final UserMatchPreferenceMapper preferenceMapper;
    private final RecommendExcludeMapper excludeMapper;
    private final RecommendEventMapper eventMapper;
    private final MatchScoreWeights weights;
    private final UserWriteGuard userWriteGuard;
    private final InMemoryRateLimiter rateLimiter;

    @Override
    public PageVO<HomeFeedVO> getRecommendFeed(Long userId, Double lat, Double lng, int page, int size) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        // 反爬限流:同用户 1 分钟最多 60 次推荐请求
        if (!rateLimiter.tryAcquire("recommend:" + userId, 60, 60_000)) {
            throw new BusinessException(ResultCode.TOO_FREQUENT);
        }
        // 分页与坐标参数校验
        if (page < 1 || size < 1 || size > 50) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分页参数不合法: page>=1, size 1-50");
        }
        validateLatLng(lat, lng);

        Set<Long> excludeIds = new HashSet<>();
        excludeIds.add(userId);
        User currentUser = userMapper.selectById(userId);
        UserMatchPreference pref = getMatchPreference(userId);

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

        // 排除"不感兴趣"的用户
        List<RecommendExclude> excludes = excludeMapper.selectList(
                new LambdaQueryWrapper<RecommendExclude>().eq(RecommendExclude::getUserId, userId));
        excludes.forEach(e -> excludeIds.add(e.getTargetUserId()));

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
        // 偏好硬性过滤(数据库层:认证/性别/城市)
        if (pref != null) {
            if (pref.getOnlyVerified() != null && pref.getOnlyVerified() == 1) {
                wrapper.eq(User::getRealNameVerified, 2);
            }
            if (pref.getPreferGender() != null && pref.getPreferGender() == 1) {
                wrapper.eq(User::getGender, 1);
            } else if (pref.getPreferGender() != null && pref.getPreferGender() == 2) {
                wrapper.eq(User::getGender, 2);
            }
            if (pref.getPreferCity() != null && !pref.getPreferCity().isBlank()) {
                wrapper.eq(User::getCity, pref.getPreferCity());
            }
            // 目标类型偏好:候选需设置相同交友目标
            if (pref.getPreferTargetType() != null && pref.getPreferTargetType() == 1) {
                wrapper.eq(User::getTargetType, 1);
            } else if (pref.getPreferTargetType() != null && pref.getPreferTargetType() == 2) {
                wrapper.eq(User::getTargetType, 2);
            }
        }

        // ── 候选全量过滤(内存:年龄/距离) → 可解释打分 → 稳定排序 → 分页 ──
        List<User> candidates = userMapper.selectList(wrapper);
        List<Scored> scored = new ArrayList<>();
        for (User c : candidates) {
            // 年龄范围过滤
            if (pref != null && ((pref.getMinAge() != null && pref.getMinAge() > 0)
                    || (pref.getMaxAge() != null && pref.getMaxAge() > 0))) {
                int age = ageOf(c);
                if (age == 0) continue;
                int min = pref.getMinAge() != null ? pref.getMinAge() : 0;
                int max = pref.getMaxAge() != null && pref.getMaxAge() > 0 ? pref.getMaxAge() : Integer.MAX_VALUE;
                if (age < min || age > max) continue;
            }
            Double dist = distanceKm(lat, lng, c);
            // 距离范围过滤
            if (pref != null && pref.getMaxDistanceKm() != null && pref.getMaxDistanceKm() > 0
                    && dist != null && dist > pref.getMaxDistanceKm()) {
                continue;
            }
            // 资料完整度门槛过滤
            if (pref != null && pref.getMinCompleteness() != null && pref.getMinCompleteness() > 0
                    && completeness(c) < pref.getMinCompleteness()) {
                continue;
            }
            scored.add(new Scored(c, scoreCandidate(currentUser, c, pref, dist)));
        }
        scored.sort((a, b) -> {
            int cmp = Integer.compare(b.score.score, a.score.score); // 得分降序
            return cmp != 0 ? cmp : Long.compare(b.user.getId(), a.user.getId()); // 同分按 id 降序(稳定)
        });

        int total = scored.size();
        // 用 long 计算偏移,避免超大 page 造成整型溢出
        long fromL = Math.min((long) (page - 1) * size, total);
        long toL = Math.min(fromL + size, total);
        List<Scored> paged = scored.subList((int) fromL, (int) toL);

        List<HomeFeedVO> records = paged.stream()
                .map(s -> toFeedVO(s.user, lat, lng, userId, s.score.reasons))
                .collect(Collectors.toList());

        // 记录曝光事件(按 user+target+type 每日去重)
        for (Scored s : paged) {
            recordEvent(userId, "expose", s.user.getId());
        }
        return PageVO.of(records, (long) total, page, size);
    }

    /** 校验经纬度范围(纬度[-90,90],经度[-180,180]),拒绝 NaN/无穷/单边缺失 */
    private void validateLatLng(Double lat, Double lng) {
        if (lat == null && lng == null) return;
        if (lat == null || lng == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "经纬度需同时提供");
        }
        if (!Double.isFinite(lat) || lat < -90 || lat > 90
                || !Double.isFinite(lng) || lng < -180 || lng > 180) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "经纬度超出合法范围");
        }
    }

    private record ScoreResult(int score, List<String> reasons) {}
    private record Scored(User user, ScoreResult score) {}

    /**
     * 可解释相亲评分:同校/同城/已认证/近期活跃/兴趣相投/距离/资料完整度,权重可配置(finding.recommend.*)。
     * 不再用"异性优先"硬编码:性别偏好由 user_match_preference.prefer_gender 在候选阶段过滤。
     */
    private ScoreResult scoreCandidate(User me, User candidate, UserMatchPreference pref, Double distanceKm) {
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (me == null) return new ScoreResult(score, reasons);

        if (me.getSchool() != null && me.getSchool().equals(candidate.getSchool())) {
            score += weights.getSameSchool();
            reasons.add("同校");
        }
        if (me.getCity() != null && candidate.getCity() != null && me.getCity().equals(candidate.getCity())) {
            score += weights.getSameCity();
            reasons.add("同城");
        }
        if (candidate.getRealNameVerified() != null && candidate.getRealNameVerified() == 2) {
            score += weights.getVerified();
            reasons.add("已认证");
        }
        if (candidate.getLastLoginAt() != null
                && candidate.getLastLoginAt().isAfter(LocalDateTime.now().minusHours(24))) {
            score += weights.getRecentActive();
            reasons.add("近期活跃");
        }
        if (candidate.getAvatar() != null && !candidate.getAvatar().isEmpty()) {
            score += weights.getHasAvatar();
        }
        // 兴趣相投(个性签名关键词)
        if (me.getSignature() != null && candidate.getSignature() != null) {
            int matches = interestMatches(me.getSignature(), candidate.getSignature());
            if (matches > 0) {
                score += weights.getInterestPerKeyword() * matches;
                reasons.add("兴趣相投");
            }
        }
        if (distanceKm != null && distanceKm < 50) {
            score += weights.getDistanceClose();
            reasons.add("距离较近");
        }
        score += weights.getCompleteness() * completeness(candidate);
        return new ScoreResult(score, reasons);
    }

    private int interestMatches(String mySig, String theirSig) {
        int matches = 0;
        for (String w : mySig.split("[，。！？,.!?\\s]+")) {
            if (w.length() >= 2 && theirSig.contains(w)) matches++;
        }
        return matches;
    }

    /** 资料完整度 0-10 */
    private int completeness(User u) {
        int filled = 0;
        if (u.getAvatar() != null && !u.getAvatar().isEmpty()) filled++;
        if (u.getSchool() != null && !u.getSchool().isEmpty()) filled++;
        if (u.getCity() != null && !u.getCity().isEmpty()) filled++;
        if (u.getGender() != null && u.getGender() > 0) filled++;
        if (u.getSignature() != null && !u.getSignature().isEmpty()) filled++;
        if (u.getBirthday() != null) filled++;
        return filled * 10 / 6;
    }

    private int ageOf(User u) {
        if (u.getBirthday() == null) return 0;
        return Period.between(u.getBirthday(), LocalDate.now()).getYears();
    }

    private Double distanceKm(Double lat, Double lng, User candidate) {
        if (lat == null || lng == null || candidate.getLatitude() == null || candidate.getLongitude() == null) {
            return null;
        }
        return GeoUtils.haversineKm(lat, lng, candidate.getLatitude().doubleValue(), candidate.getLongitude().doubleValue());
    }

    @Override
    @Transactional
    public void applyChat(Long fromUserId, Long toUserId, String remark) {
        userWriteGuard.checkWritable(fromUserId);
        // 反骚扰限流:同一用户 1 小时内申请上限(冷却期之外的额外频率限制)
        if (!rateLimiter.tryAcquire("apply:" + fromUserId, 10, 3_600_000)) {
            throw new BusinessException(ResultCode.TOO_FREQUENT);
        }
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能给自己发送申请");
        }
        // Check real-name verification
        verificationGuard.checkVerified(fromUserId);

        // 目标账号必须存在
        User targetUser = userMapper.selectById(toUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 统一发现权限:目标账号状态(status==1)/可搜索/双向拉黑(复用 canDiscover)
        if (!relationshipService.canDiscover(fromUserId, toUserId)) {
            if (relationshipService.isBlockedEitherWay(fromUserId, toUserId)) {
                throw new BusinessException(ResultCode.RELATION_BLOCKED);
            }
            throw new BusinessException(ResultCode.USER_NOT_DISCOVERABLE);
        }
        // 统一申请权限:加好友方式(friendAddMode==2 不可申请,复用 canApplyChat)
        if (!relationshipService.canApplyChat(fromUserId, toUserId)) {
            throw new BusinessException(ResultCode.CONTACT_PERMISSION_DENIED);
        }

        // 加好友方式:2=不允许申请直接拒绝;0=所有人可申请(自动通过);1=需验证(默认)
        UserSettings targetSettings = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, toUserId));
        int friendMode = targetSettings != null && targetSettings.getFriendAddMode() != null
                ? targetSettings.getFriendAddMode() : 1;

        // 冷却期:同一方向最近一次被拒绝/撤回后 7 天内不能重发
        LambdaQueryWrapper<ChatApply> cooldownW = new LambdaQueryWrapper<ChatApply>()
                .eq(ChatApply::getFromUserId, fromUserId)
                .eq(ChatApply::getToUserId, toUserId)
                .in(ChatApply::getStatus, ChatApplyStatus.REJECTED.getCode(), ChatApplyStatus.CANCELLED.getCode())
                .isNotNull(ChatApply::getHandleTime)
                .orderByDesc(ChatApply::getHandleTime)
                .last("LIMIT 1");
        ChatApply lastRejected = chatApplyMapper.selectOne(cooldownW);
        if (lastRejected != null && lastRejected.getHandleTime() != null
                && lastRejected.getHandleTime().isAfter(LocalDateTime.now().minusDays(APPLY_COOLDOWN_DAYS))) {
            throw new BusinessException(ResultCode.CHAT_APPLY_COOLDOWN);
        }

        // 已有待处理申请 → 拒绝重复申请
        Long pendingCount = chatApplyMapper.selectCount(new LambdaQueryWrapper<ChatApply>()
                .eq(ChatApply::getFromUserId, fromUserId)
                .eq(ChatApply::getToUserId, toUserId)
                .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode()));
        if (pendingCount > 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_SENT);
        }

        // Insert application(pending_key 唯一约束兜底并发:同一方向同时只有一条待处理)
        ChatApply apply = new ChatApply();
        apply.setFromUserId(fromUserId);
        apply.setToUserId(toUserId);
        apply.setStatus(ChatApplyStatus.PENDING.getCode());
        apply.setRemark(remark);
        apply.setApplyTime(LocalDateTime.now());
        // 申请备注含违禁词直接拒绝
        sensitiveWordFilter.assertClean(remark);
        try {
            chatApplyMapper.insert(apply);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_SENT);
        }
        // 行为事件:申请
        recordEvent(fromUserId, "apply", toUserId);

        if (friendMode == 0) {
            // 所有人可申请 → 自动通过并建立会话
            apply.setHandleBy(toUserId); // 由接收方设置自动通过
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
        expireStalePending();
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
        expireStalePending();
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
        expireStalePending();
        return chatApplyMapper.selectCount(new LambdaQueryWrapper<ChatApply>()
                .eq(ChatApply::getToUserId, userId)
                .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode()));
    }

    @Override
    @Transactional
    public void handleApply(Long userId, Long applyId, Integer status) {
        expireStalePending();
        ChatApply apply = chatApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.CHAT_APPLY_NOT_FOUND);
        }
        // Only the receiver can handle
        if (!apply.getToUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权处理该申请");
        }
        // 待处理超期 → 惰性过期,接收人不可再处理
        if (apply.getStatus() == ChatApplyStatus.PENDING.getCode()
                && apply.getApplyTime().isBefore(LocalDateTime.now().minusDays(APPLY_EXPIRE_DAYS))) {
            chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                    .eq(ChatApply::getId, applyId)
                    .set(ChatApply::getStatus, ChatApplyStatus.EXPIRED.getCode())
                    .set(ChatApply::getHandleTime, LocalDateTime.now()));
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_HANDLED, "申请已过期，无法处理");
        }
        // 审批时再次检查拉黑:拉黑后待处理申请作废
        if (relationshipService.isBlockedEitherWay(apply.getFromUserId(), apply.getToUserId())) {
            chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                    .eq(ChatApply::getId, applyId)
                    .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode())
                    .set(ChatApply::getStatus, ChatApplyStatus.CANCELLED.getCode())
                    .set(ChatApply::getHandleTime, LocalDateTime.now())
                    .set(ChatApply::getHandleBy, userId));
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }
        // 审批时再次校验双方账号状态:任一被封禁/注销/冻结,待处理申请作废
        User applicant = userMapper.selectById(apply.getFromUserId());
        User receiver = userMapper.selectById(apply.getToUserId());
        if (applicant == null || receiver == null
                || applicant.getStatus() == null || applicant.getStatus() != 1
                || receiver.getStatus() == null || receiver.getStatus() != 1) {
            chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                    .eq(ChatApply::getId, applyId)
                    .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode())
                    .set(ChatApply::getStatus, ChatApplyStatus.CANCELLED.getCode())
                    .set(ChatApply::getHandleTime, LocalDateTime.now())
                    .set(ChatApply::getHandleBy, userId));
            throw new BusinessException(ResultCode.USER_NOT_DISCOVERABLE, "对方账号状态异常，无法处理");
        }
        // 条件更新:仅 status=PENDING 才能处理,保证并发下只有一次成功
        int rows = chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                .eq(ChatApply::getId, applyId)
                .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode())
                .set(ChatApply::getStatus, status)
                .set(ChatApply::getHandleTime, LocalDateTime.now())
                .set(ChatApply::getHandleBy, userId));
        if (rows == 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_HANDLED);
        }

        if (status == ChatApplyStatus.APPROVED.getCode()) {
            // 通过:建会话 + 系统消息 + 通知申请人(仅一次)
            approveApply(apply);
            recordEvent(userId, "approve", apply.getFromUserId());
        } else {
            User handler = userMapper.selectById(userId);
            messageService.notify(userId, apply.getFromUserId(), "chat_rejected",
                    "你的聊天申请已拒绝" + (handler != null ? "（" + handler.getNickname() + "）" : ""), applyId);
        }
    }

    @Override
    @Transactional
    public void withdrawApply(Long userId, Long applyId) {
        expireStalePending();
        ChatApply apply = chatApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.CHAT_APPLY_NOT_FOUND);
        }
        // Only the applicant can withdraw
        if (!apply.getFromUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权撤回该申请");
        }
        // 条件更新:仅待处理可撤回
        int rows = chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                .eq(ChatApply::getId, applyId)
                .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode())
                .set(ChatApply::getStatus, ChatApplyStatus.CANCELLED.getCode())
                .set(ChatApply::getHandleTime, LocalDateTime.now())
                .set(ChatApply::getHandleBy, userId));
        if (rows == 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_HANDLED, "申请已处理，无法撤回");
        }
    }

    /** 惰性过期:超过期限未处理的待申请置为 EXPIRED(供列表/计数/处理前调用) */
    private void expireStalePending() {
        chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode())
                .lt(ChatApply::getApplyTime, LocalDateTime.now().minusDays(APPLY_EXPIRE_DAYS))
                .set(ChatApply::getStatus, ChatApplyStatus.EXPIRED.getCode())
                .set(ChatApply::getHandleTime, LocalDateTime.now()));
    }

    // ── 相亲交友偏好 ──

    @Override
    public UserMatchPreference getMatchPreference(Long userId) {
        UserMatchPreference p = preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserMatchPreference>().eq(UserMatchPreference::getUserId, userId));
        if (p == null) {
            p = new UserMatchPreference();
            p.setUserId(userId);
            p.setPreferGender(0);
            p.setMinAge(0);
            p.setMaxAge(0);
            p.setMaxDistanceKm(0);
            p.setOnlyVerified(0);
            p.setPreferTargetType(0);
            p.setMinCompleteness(0);
        }
        return p;
    }

    @Override
    @Transactional
    public void updateMatchPreference(Long userId, UserMatchPreference pref) {
        pref.setUserId(userId);
        if (pref.getPreferGender() == null) pref.setPreferGender(0);
        if (pref.getMinAge() == null) pref.setMinAge(0);
        if (pref.getMaxAge() == null) pref.setMaxAge(0);
        if (pref.getMaxDistanceKm() == null) pref.setMaxDistanceKm(0);
        if (pref.getOnlyVerified() == null) pref.setOnlyVerified(0);
        if (pref.getPreferTargetType() == null) pref.setPreferTargetType(0);
        if (pref.getMinCompleteness() == null) pref.setMinCompleteness(0);
        if (pref.getPreferGender() < 0 || pref.getPreferGender() > 2) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "preferGender 仅允许 0/1/2");
        }
        if (pref.getPreferTargetType() < 0 || pref.getPreferTargetType() > 2) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "preferTargetType 仅允许 0/1/2");
        }
        if (pref.getMinCompleteness() < 0 || pref.getMinCompleteness() > 10) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "minCompleteness 仅允许 0-10");
        }
        if (pref.getMinAge() < 0 || pref.getMaxAge() < 0
                || (pref.getMaxAge() > 0 && pref.getMinAge() > pref.getMaxAge())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "年龄范围不合法");
        }
        UserMatchPreference existing = preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserMatchPreference>().eq(UserMatchPreference::getUserId, userId));
        if (existing == null) {
            preferenceMapper.insert(pref);
        } else {
            pref.setId(existing.getId());
            preferenceMapper.updateById(pref);
        }
    }

    // ── 不感兴趣 + 行为事件 ──

    @Override
    @Transactional
    public void skipUser(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) return;
        Long exists = excludeMapper.selectCount(new LambdaQueryWrapper<RecommendExclude>()
                .eq(RecommendExclude::getUserId, userId)
                .eq(RecommendExclude::getTargetUserId, targetUserId));
        if (exists == null || exists == 0) {
            RecommendExclude e = new RecommendExclude();
            e.setUserId(userId);
            e.setTargetUserId(targetUserId);
            excludeMapper.insert(e);
        }
        recordEvent(userId, "skip", targetUserId);
    }

    private void recordEvent(Long userId, String type, Long targetUserId) {
        RecommendEvent ev = new RecommendEvent();
        ev.setUserId(userId);
        ev.setEventType(type);
        ev.setTargetUserId(targetUserId);
        try {
            eventMapper.insert(ev);
        } catch (DuplicateKeyException e) {
            // dedup_key 唯一约束:同日同 user+target+type 事件已存在,幂等忽略(如重复曝光)
        }
    }

    // ── Private helpers ──

    /** 通过聊天申请:置状态 + 建会话/系统消息 + 通知申请人(幂等,并发下仅一次成功) */
    private void approveApply(ChatApply apply) {
        apply.setStatus(ChatApplyStatus.APPROVED.getCode());
        apply.setHandleTime(LocalDateTime.now());
        chatApplyMapper.updateById(apply);

        try {
            // 批准后创建会话(唯一入口:新会话只能由聊天申请批准流程建立)
            var convVO = chatService.createConversation(apply.getToUserId(), apply.getFromUserId());
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

    /** 更新/创建 contact 记录(uk_uid_room 唯一约束下幂等,并发重复插入忽略) */
    private void updateContact(Long uid, Long roomId, Long msgId) {
        Contact contact = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, uid)
                        .eq(Contact::getRoomId, roomId));
        if (contact == null) {
            try {
                contact = new Contact();
                contact.setUid(uid);
                contact.setRoomId(roomId);
                contact.setActiveTime(LocalDateTime.now());
                contact.setLastMsgId(msgId);
                contactMapper.insert(contact);
            } catch (DuplicateKeyException e) {
                contact = contactMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contact>()
                                .eq(Contact::getUid, uid)
                                .eq(Contact::getRoomId, roomId));
                if (contact != null) {
                    contact.setActiveTime(LocalDateTime.now());
                    contact.setLastMsgId(msgId);
                    contact.setHidden(0); // 新消息/新会话 → 隐藏会话自动恢复
                    contactMapper.updateById(contact);
                }
            }
        } else {
            contact.setActiveTime(LocalDateTime.now());
            contact.setLastMsgId(msgId);
            contact.setHidden(0); // 新消息/新会话 → 隐藏会话自动恢复
            contactMapper.updateById(contact);
        }
    }


    private HomeFeedVO toFeedVO(User user, Double lat, Double lng, Long currentUserId, List<String> matchReasons) {
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
        vo.setMatchReasons(matchReasons);

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
        return ChatApplyStatus.descOf(status);
    }
}
