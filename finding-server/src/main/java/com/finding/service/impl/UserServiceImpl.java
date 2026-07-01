package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.dto.PageQueryDTO;
import com.finding.entity.User;
import com.finding.entity.UserFollow;
import com.finding.mapper.PostMapper;
import com.finding.mapper.UserFollowMapper;
import com.finding.mapper.UserMapper;
import com.finding.service.UserService;
import com.finding.vo.PageVO;
import com.finding.vo.UserVO;
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
    private final PostMapper postMapper;

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
        vo.setPostCount(postMapper.selectCount(
                new LambdaQueryWrapper<com.finding.entity.Post>()
                        .eq(com.finding.entity.Post::getUserId, userId)
                        .eq(com.finding.entity.Post::getStatus, 1)).intValue());

        if (currentUserId != null) {
            vo.setIsFollowed(isFollowing(currentUserId, userId));
        }

        return vo;
    }

    @Override
    public PageVO<UserVO> searchUsers(String keyword, PageQueryDTO pageQuery) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getSchool, keyword));
        }
        wrapper.orderByDesc(User::getLastLoginAt);

        Page<User> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<User> result = userMapper.selectPage(page, wrapper);

        List<UserVO> records = result.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
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
    public PageVO<UserVO> getFollowers(Long userId, PageQueryDTO pageQuery) {
        Page<UserFollow> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<UserFollow> result = followMapper.selectPage(page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFolloweeId, userId)
                        .orderByDesc(UserFollow::getCreatedAt));

        Set<Long> followerIds = result.getRecords().stream()
                .map(UserFollow::getFollowerId).collect(Collectors.toSet());
        if (followerIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }

        List<User> users = userMapper.selectBatchIds(followerIds);
        List<UserVO> records = users.stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
    }

    @Override
    public PageVO<UserVO> getFollowing(Long userId, PageQueryDTO pageQuery) {
        Page<UserFollow> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<UserFollow> result = followMapper.selectPage(page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .orderByDesc(UserFollow::getCreatedAt));

        Set<Long> followeeIds = result.getRecords().stream()
                .map(UserFollow::getFolloweeId).collect(Collectors.toSet());
        if (followeeIds.isEmpty()) {
            return PageVO.of(List.of(), 0L, pageQuery.getPage(), pageQuery.getSize());
        }

        List<User> users = userMapper.selectBatchIds(followeeIds);
        List<UserVO> records = users.stream().map(this::toVO).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
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
        vo.setGender(user.getGender());
        vo.setSchool(user.getSchool());
        vo.setSignature(user.getSignature());
        vo.setCity(user.getCity());
        vo.setRealNameVerified(user.getRealNameVerified());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
