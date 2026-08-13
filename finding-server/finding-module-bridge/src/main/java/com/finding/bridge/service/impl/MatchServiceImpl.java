package com.finding.bridge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.bridge.entity.UserLike;
import com.finding.bridge.entity.UserMatch;
import com.finding.bridge.mapper.UserLikeMapper;
import com.finding.bridge.mapper.UserMatchMapper;
import com.finding.bridge.service.MatchService;
import com.finding.bridge.vo.MatchUserVO;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.message.service.MessageService;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserRelationshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    /** 配对成功后推送的见面安全提醒 */
    private static final String SAFETY_REMINDER =
            "温馨提示：与线上认识的朋友线下见面时，请选择公共场所，并提前告知亲友你的行踪，注意人身与财产安全。";

    private final UserLikeMapper userLikeMapper;
    private final UserMatchMapper userMatchMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final UserRelationshipService relationshipService;

    @Override
    @Transactional
    public boolean likeUser(Long userId, Long targetId) {
        if (targetId == null || userId.equals(targetId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能对自己心动");
        }
        User target = userMapper.selectById(targetId);
        if (target == null || target.getStatus() == null || target.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (relationshipService.isBlockedEitherWay(userId, targetId)) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }

        // 幂等:已心动则跳过插入(唯一键 uk_liker_liked 兜底并发)
        try {
            UserLike like = new UserLike();
            like.setLikerId(userId);
            like.setLikedId(targetId);
            userLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 已存在,忽略
        }

        // 对方也喜欢我 → 配对
        if (existsLike(targetId, userId)) {
            if (ensureMatch(userId, targetId)) {
                notifyMatch(userId, targetId);
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void unlikeUser(Long userId, Long targetId) {
        if (targetId == null || userId.equals(targetId)) return;
        userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getLikerId, userId)
                .eq(UserLike::getLikedId, targetId));
        // 已配对则解除配对
        long lo = Math.min(userId, targetId);
        long hi = Math.max(userId, targetId);
        userMatchMapper.delete(new LambdaQueryWrapper<UserMatch>()
                .eq(UserMatch::getUserAId, lo)
                .eq(UserMatch::getUserBId, hi));
    }

    @Override
    public PageVO<MatchUserVO> getMyLikes(Long userId, int page, int size) {
        Page<UserLike> result = userLikeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getLikerId, userId)
                        .orderByDesc(UserLike::getCreatedAt));
        List<Long> likedIds = result.getRecords().stream().map(UserLike::getLikedId).collect(Collectors.toList());
        Map<Long, LocalDateTime> timeByUser = result.getRecords().stream()
                .collect(Collectors.toMap(UserLike::getLikedId, UserLike::getCreatedAt, (a, b) -> a));
        Set<Long> matched = likesMeAmong(likedIds, userId);
        return toVO(result, likedIds, timeByUser, matched);
    }

    @Override
    public PageVO<MatchUserVO> getLikesReceived(Long userId, int page, int size) {
        Page<UserLike> result = userLikeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getLikedId, userId)
                        .orderByDesc(UserLike::getCreatedAt));
        List<Long> likerIds = result.getRecords().stream().map(UserLike::getLikerId).collect(Collectors.toList());
        Map<Long, LocalDateTime> timeByUser = result.getRecords().stream()
                .collect(Collectors.toMap(UserLike::getLikerId, UserLike::getCreatedAt, (a, b) -> a));
        Set<Long> matched = myLikesAmong(userId, likerIds);
        return toVO(result, likerIds, timeByUser, matched);
    }

    @Override
    public PageVO<MatchUserVO> getMyMatches(Long userId, int page, int size) {
        Page<UserMatch> result = userMatchMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserMatch>()
                        .and(w -> w.eq(UserMatch::getUserAId, userId).or().eq(UserMatch::getUserBId, userId))
                        .orderByDesc(UserMatch::getMatchedAt));
        List<Long> otherIds = new ArrayList<>();
        Map<Long, LocalDateTime> timeByUser = new HashMap<>();
        for (UserMatch m : result.getRecords()) {
            Long other = userId.equals(m.getUserAId()) ? m.getUserBId() : m.getUserAId();
            otherIds.add(other);
            timeByUser.put(other, m.getMatchedAt());
        }
        Set<Long> matched = new HashSet<>(otherIds);
        return toVO(result, otherIds, timeByUser, matched);
    }

    // ── helpers ──

    private boolean existsLike(Long likerId, Long likedId) {
        Long c = userLikeMapper.selectCount(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getLikerId, likerId)
                .eq(UserLike::getLikedId, likedId));
        return c != null && c > 0;
    }

    /** 规范化 a<b 后插入配对记录;返回是否「新创建」(已存在返回 false) */
    private boolean ensureMatch(Long a, Long b) {
        long lo = Math.min(a, b);
        long hi = Math.max(a, b);
        Long c = userMatchMapper.selectCount(new LambdaQueryWrapper<UserMatch>()
                .eq(UserMatch::getUserAId, lo)
                .eq(UserMatch::getUserBId, hi));
        if (c != null && c > 0) return false;
        try {
            UserMatch m = new UserMatch();
            m.setUserAId(lo);
            m.setUserBId(hi);
            m.setMatchedAt(LocalDateTime.now());
            userMatchMapper.insert(m);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private void notifyMatch(Long a, Long b) {
        User ua = userMapper.selectById(a);
        User ub = userMapper.selectById(b);
        String na = ua != null ? ua.getNickname() : "对方";
        String nb = ub != null ? ub.getNickname() : "对方";
        messageService.notify(a, b, "match", "你和 " + na + " 互相喜欢，配对成功！", null);
        messageService.notify(b, a, "match", "你和 " + nb + " 互相喜欢，配对成功！", null);
        // 见面安全提醒
        messageService.notify(a, b, "safety_reminder", SAFETY_REMINDER, null);
        messageService.notify(b, a, "safety_reminder", SAFETY_REMINDER, null);
    }

    /** candidates 中哪些人喜欢 me */
    private Set<Long> likesMeAmong(Collection<Long> candidates, Long me) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptySet();
        return userLikeMapper.selectList(new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getLikedId, me)
                        .in(UserLike::getLikerId, candidates))
                .stream().map(UserLike::getLikerId).collect(Collectors.toSet());
    }

    /** me 喜欢 candidates 中的哪些人 */
    private Set<Long> myLikesAmong(Long me, Collection<Long> candidates) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptySet();
        return userLikeMapper.selectList(new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getLikerId, me)
                        .in(UserLike::getLikedId, candidates))
                .stream().map(UserLike::getLikedId).collect(Collectors.toSet());
    }

    private PageVO<MatchUserVO> toVO(Page<?> result, List<Long> ids,
                                     Map<Long, LocalDateTime> timeByUser, Set<Long> matched) {
        List<User> users = ids.isEmpty() ? List.of() : userMapper.selectBatchIds(ids);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<MatchUserVO> records = new ArrayList<>();
        for (Long id : ids) {
            User u = userMap.get(id);
            if (u == null) continue;
            records.add(toMatchUserVO(u, timeByUser.get(id), matched.contains(id)));
        }
        return PageVO.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    private MatchUserVO toMatchUserVO(User u, LocalDateTime time, boolean matched) {
        MatchUserVO vo = new MatchUserVO();
        vo.setUserId(u.getId());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setGender(u.getGender());
        vo.setSchool(u.getSchool());
        vo.setSignature(u.getSignature());
        vo.setVerified(u.getRealNameVerified() != null && u.getRealNameVerified() == 2 ? 1 : 0);
        vo.setTargetType(u.getTargetType());
        vo.setTime(time);
        vo.setIsMatched(matched);
        return vo;
    }
}
