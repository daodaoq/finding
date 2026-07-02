package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.entity.*;
import com.finding.mapper.*;
import com.finding.service.HomeService;
import com.finding.utils.GeoUtils;
import com.finding.vo.HomeFeedVO;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;
    private final BannerMapper bannerMapper;
    private final MessageMapper messageMapper;

    @Override
    public PageVO<HomeFeedVO> getRecommendFeed(Long userId, Double lat, Double lng, int page, int size) {
        // Get followed user IDs to exclude from results
        Set<Long> excludeIds = new HashSet<>();
        excludeIds.add(userId);
        List<UserFollow> follows = followMapper.selectList(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));
        follows.forEach(f -> excludeIds.add(f.getFolloweeId()));

        // Get active users, prefer verified ones
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .notIn(User::getId, excludeIds)
                .orderByDesc(User::getRealNameVerified, User::getLastLoginAt);

        Page<User> pg = new Page<>(page, size);
        Page<User> result = userMapper.selectPage(pg, wrapper);

        List<HomeFeedVO> records = result.getRecords().stream()
                .map(u -> toFeedVO(u, lat, lng))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void likeUser(Long userId, Long targetUserId) {
        // 创建关注关系
        if (followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFolloweeId, targetUserId)) == 0) {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(userId);
            follow.setFolloweeId(targetUserId);
            followMapper.insert(follow);
        }

        // 发送"喜欢"通知
        User fromUser = userMapper.selectById(userId);
        Message msg = new Message();
        msg.setFromUserId(userId);
        msg.setToUserId(targetUserId);
        msg.setType("like");
        msg.setContent((fromUser != null ? fromUser.getNickname() : "有人") + "对你表示了喜欢");
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    @Override
    public List<Map<String, Object>> getBanners() {
        List<Banner> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getIsActive, 1)
                        .orderByAsc(Banner::getSortOrder));

        return banners.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("title", b.getTitle());
            map.put("imageUrl", b.getImageUrl());
            map.put("linkUrl", b.getLinkUrl());
            return map;
        }).collect(Collectors.toList());
    }

    private HomeFeedVO toFeedVO(User user, Double lat, Double lng) {
        HomeFeedVO vo = new HomeFeedVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setSchool(user.getSchool());
        vo.setSignature(user.getSignature());
        vo.setCity(user.getCity());
        vo.setLastLoginAt(user.getLastLoginAt());

        if (lat != null && lng != null && user.getLatitude() != null && user.getLongitude() != null) {
            vo.setDistanceKm(GeoUtils.haversineKm(lat, lng,
                    user.getLatitude().doubleValue(), user.getLongitude().doubleValue()));
        }
        return vo;
    }
}
