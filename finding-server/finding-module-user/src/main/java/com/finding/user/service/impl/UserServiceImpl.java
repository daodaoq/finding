package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.PageQueryDTO;
import com.finding.user.entity.User;
import com.finding.user.entity.UserFollow;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.service.UserPostStatsQuery;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserService;
import com.finding.common.PageVO;
import com.finding.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;
    private final UserPostStatsQuery userPostStatsQuery;
    private final UserSettingsMapper userSettingsMapper;
    private final UserRelationshipService relationshipService;

    @Override
    public UserVO getUserProfile(Long userId, Long currentUserId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        UserVO vo = toVO(user);

        // Counts
        vo.setFollowerCount(followMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFolloweeId, userId)).intValue());
        vo.setFollowingCount(followMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId)).intValue());
        vo.setPostCount(userPostStatsQuery.countPosts(userId));
        // 互关(好友)数:关注我的人中,我也关注了他们
        vo.setMutualCount(followMapper.selectCount(
                        new LambdaQueryWrapper<UserFollow>()
                                .eq(UserFollow::getFolloweeId, userId)
                                .inSql(UserFollow::getFollowerId,
                                        "SELECT followee_id FROM user_follow WHERE follower_id = " + userId))
                .intValue());

        if (currentUserId != null) {
            vo.setIsFollowed(isFollowing(currentUserId, userId));
        }

        // 资料可见性 + 拉黑:不可查看详细资料时,隐藏性别/城市/签名(仅保留公开资料)
        if (currentUserId != null && !userId.equals(currentUserId)) {
            relationshipService.projectDetailedFields(currentUserId, userId, vo);
        }

        return vo;
    }

    @Override
    public PageVO<UserVO> searchUsers(String keyword, PageQueryDTO pageQuery, Long currentUserId) {
        // 排除关闭"允许被搜索"的用户 + 与当前用户存在拉黑关系的用户 + 自己
        List<Long> hiddenIds = userSettingsMapper.selectList(
                        new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getSearchable, 0))
                .stream().map(UserSettings::getUserId).toList();
        Set<Long> blockedIds = currentUserId != null ? relationshipService.blockedUserIds(currentUserId) : Set.of();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .ne(currentUserId != null, User::getId, currentUserId)
                .notIn(!hiddenIds.isEmpty(), User::getId, hiddenIds)
                .notIn(!blockedIds.isEmpty(), User::getId, blockedIds);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getSchool, keyword));
        }
        wrapper.orderByDesc(User::getLastLoginAt);

        Page<User> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<User> result = userMapper.selectPage(page, wrapper);

        List<UserVO> records = result.getRecords().stream().map(u -> {
            UserVO vo = toVO(u);
            relationshipService.projectDetailedFields(currentUserId, u.getId(), vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
    }

    @Override
    @Transactional
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessException(ResultCode.CANNOT_FOLLOW_SELF);
        }
        if (userMapper.selectById(followeeId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!relationshipService.canFollow(followerId, followeeId)) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }

        // 已关注 → 取消关注；未关注 → 关注
        UserFollow existing = followMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));

        if (existing != null) {
            followMapper.deleteById(existing.getId());
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            followMapper.insert(follow);
        }
    }

    @Override
    public void unfollowUser(Long followerId, Long followeeId) {
        followMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));
    }

    @Override
    public PageVO<UserVO> getFollowers(Long userId, PageQueryDTO pageQuery, Long currentUserId) {
        Page<UserFollow> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<UserFollow> result = followMapper.selectPage(page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFolloweeId, userId)
                        .orderByDesc(UserFollow::getCreatedAt));

        List<Long> followerIds = result.getRecords().stream()
                .map(UserFollow::getFollowerId).collect(Collectors.toList());
        if (followerIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }

        // 按当前访问者过滤被拉黑对象
        List<Long> visibleIds = relationshipService.filterNotBlocked(currentUserId, followerIds);
        if (visibleIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }

        List<User> users = userMapper.selectBatchIds(visibleIds);
        List<UserVO> records = users.stream().map(u -> {
            UserVO vo = toVO(u);
            // 检查我是否也关注了ta → 互关
            vo.setIsFollowed(isFollowing(userId, u.getId()));
            relationshipService.projectDetailedFields(currentUserId, u.getId(), vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
    }

    @Override
    public PageVO<UserVO> getFollowing(Long userId, PageQueryDTO pageQuery, Long currentUserId) {
        Page<UserFollow> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<UserFollow> result = followMapper.selectPage(page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .orderByDesc(UserFollow::getCreatedAt));

        List<Long> followeeIds = result.getRecords().stream()
                .map(UserFollow::getFolloweeId).collect(Collectors.toList());
        if (followeeIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }

        // 按当前访问者过滤被拉黑对象
        List<Long> visibleIds = relationshipService.filterNotBlocked(currentUserId, followeeIds);
        if (visibleIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }

        List<User> users = userMapper.selectBatchIds(visibleIds);
        List<UserVO> records = users.stream().map(u -> {
            UserVO vo = toVO(u);
            // 检查对方是否也关注了我 → 互关
            boolean mutual = followMapper.selectCount(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getFollowerId, u.getId())
                            .eq(UserFollow::getFolloweeId, userId)) > 0;
            vo.setIsFollowed(mutual); // true=互关 false=仅我关注ta
            relationshipService.projectDetailedFields(currentUserId, u.getId(), vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
    }

    @Override
    public PageVO<UserVO> getMutualFollows(Long userId, PageQueryDTO pageQuery, Long currentUserId) {
        // 我关注的人
        List<UserFollow> following = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId));
        if (following.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }
        Set<Long> followingIds = following.stream()
                .map(UserFollow::getFolloweeId).collect(Collectors.toSet());
        // 关注我的人
        List<UserFollow> followers = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFolloweeId, userId));
        Set<Long> followerIds = followers.stream()
                .map(UserFollow::getFollowerId).collect(Collectors.toSet());

        // 交集 = 互相关注
        followingIds.retainAll(followerIds);
        if (followingIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }

        // 按当前访问者过滤被拉黑对象
        List<Long> mutualIds = relationshipService.filterNotBlocked(currentUserId, followingIds);
        if (mutualIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }
        int total = mutualIds.size();
        int from = Math.min((pageQuery.getPage() - 1) * pageQuery.getSize(), total);
        int to = Math.min(from + pageQuery.getSize(), total);
        List<Long> pagedIds = mutualIds.subList(from, to);

        List<User> users = userMapper.selectBatchIds(pagedIds);
        List<UserVO> records = users.stream().map(u -> {
            UserVO vo = toVO(u);
            vo.setIsFollowed(true); // 列表里的都是互相关注
            relationshipService.projectDetailedFields(currentUserId, u.getId(), vo);
            return vo;
        }).collect(Collectors.toList());
        return PageVO.of(records, (long) total, pageQuery.getPage(), pageQuery.getSize());
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId)) > 0;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setProfileBackground(user.getProfileBackground());
        vo.setGender(user.getGender());
        vo.setSchool(user.getSchool());
        vo.setSignature(user.getSignature());
        vo.setCity(user.getCity());
        vo.setRealNameVerified(user.getRealNameVerified());
        vo.setTargetType(user.getTargetType());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
