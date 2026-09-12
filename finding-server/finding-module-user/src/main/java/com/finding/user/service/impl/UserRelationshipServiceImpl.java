package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.user.entity.User;
import com.finding.user.entity.UserBlock;
import com.finding.user.entity.UserRemark;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserBlockMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserRemarkMapper;
import com.finding.user.service.InfoShareQuery;
import com.finding.user.service.RemarkCache;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRelationshipServiceImpl implements UserRelationshipService {

    private final UserMapper userMapper;
    private final UserBlockMapper userBlockMapper;
    private final UserSettingsService userSettingsService;
    private final InfoShareQuery infoShareQuery;
    /** 直接注入 mapper 而非 UserRemarkService,避免两个 service 相互依赖 */
    private final UserRemarkMapper userRemarkMapper;
    private final RemarkCache remarkCache;

    @Override
    public boolean isBlockedEitherWay(Long userId, Long targetId) {
        if (userId == null || targetId == null || userId.equals(targetId)) return false;
        Long c = userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .and(w -> w.eq(UserBlock::getUserId, userId).eq(UserBlock::getBlockedUserId, targetId)
                        .or().eq(UserBlock::getUserId, targetId).eq(UserBlock::getBlockedUserId, userId)));
        return c != null && c > 0;
    }

    @Override
    public Set<Long> blockedUserIds(Long userId) {
        if (userId == null) return Set.of();
        List<UserBlock> blocks = userBlockMapper.selectList(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .or().eq(UserBlock::getBlockedUserId, userId));
        Set<Long> ids = new HashSet<>();
        for (UserBlock b : blocks) {
            if (userId.equals(b.getUserId())) ids.add(b.getBlockedUserId());
            else ids.add(b.getUserId());
        }
        return ids;
    }

    @Override
    public List<Long> filterNotBlocked(Long userId, Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) return List.of();
        if (userId == null) return new ArrayList<>(targetIds); // 匿名访问:无拉黑概念,全部放行
        Set<Long> blocked = blockedUserIds(userId);
        return targetIds.stream().filter(id -> !blocked.contains(id)).collect(Collectors.toList());
    }

    @Override
    public boolean canDiscover(Long visitorId, Long targetId) {
        if (visitorId == null || targetId == null || visitorId.equals(targetId)) return false;
        if (isBlockedEitherWay(visitorId, targetId)) return false;
        User target = userMapper.selectById(targetId);
        if (target == null || target.getStatus() == null || target.getStatus() != 1) return false;
        UserSettings s = userSettingsService.getSettings(targetId);
        return s.getSearchable() == null || s.getSearchable() == 1;
    }

    @Override
    public boolean canViewDetailedProfile(Long visitorId, Long targetId) {
        if (visitorId == null || targetId == null) return false;
        if (visitorId.equals(targetId)) return true;
        if (isBlockedEitherWay(visitorId, targetId)) return false;
        UserSettings s = userSettingsService.getSettings(targetId);
        if (s.getProfileVisible() != null && s.getProfileVisible() == 2) {
            return infoShareQuery.hasApprovedShare(visitorId, targetId);
        }
        return true;
    }

    @Override
    public boolean canFollow(Long visitorId, Long targetId) {
        if (visitorId == null || targetId == null || visitorId.equals(targetId)) return false;
        return !isBlockedEitherWay(visitorId, targetId);
    }

    @Override
    public boolean canApplyChat(Long visitorId, Long targetId) {
        if (visitorId == null || targetId == null || visitorId.equals(targetId)) return false;
        if (isBlockedEitherWay(visitorId, targetId)) return false;
        UserSettings s = userSettingsService.getSettings(targetId);
        return s.getFriendAddMode() == null || s.getFriendAddMode() != 2;
    }

    @Override
    public String remarkOf(Long viewerId, Long targetId) {
        if (viewerId == null || targetId == null || viewerId.equals(targetId)) return null;
        String cached = remarkCache.get(viewerId, targetId);
        if (cached != null) return cached.isEmpty() ? null : cached;

        UserRemark row = userRemarkMapper.selectOne(new LambdaQueryWrapper<UserRemark>()
                .select(UserRemark::getRemark)
                .eq(UserRemark::getUserId, viewerId)
                .eq(UserRemark::getTargetUserId, targetId));
        String remark = row == null ? "" : row.getRemark();
        remarkCache.put(viewerId, targetId, remark);
        return remark.isEmpty() ? null : remark;
    }

    @Override
    public Map<Long, String> remarkMap(Long viewerId, Collection<Long> targetIds) {
        if (viewerId == null || targetIds == null || targetIds.isEmpty()) return Map.of();
        List<Long> ids = targetIds.stream()
                .filter(id -> id != null && !id.equals(viewerId))
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return Map.of();

        List<UserRemark> rows = userRemarkMapper.selectList(new LambdaQueryWrapper<UserRemark>()
                .eq(UserRemark::getUserId, viewerId)
                .in(UserRemark::getTargetUserId, ids));

        Map<Long, String> map = new HashMap<>();
        for (UserRemark row : rows) {
            String remark = row.getRemark();
            map.put(row.getTargetUserId(), remark);
            remarkCache.put(viewerId, row.getTargetUserId(), remark);
        }
        map.values().removeIf(r -> r == null || r.isBlank());
        return map;
    }
}
