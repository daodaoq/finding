package com.finding.user.service;

import com.finding.common.PageQueryDTO;
import com.finding.common.PageVO;
import com.finding.user.vo.UserVO;

public interface UserService {

    UserVO getUserProfile(Long userId, Long currentUserId);
    PageVO<UserVO> searchUsers(String keyword, PageQueryDTO pageQuery, Long currentUserId);
    void followUser(Long followerId, Long followeeId);
    void unfollowUser(Long followerId, Long followeeId);
    PageVO<UserVO> getFollowers(Long userId, PageQueryDTO pageQuery, Long currentUserId);
    PageVO<UserVO> getFollowing(Long userId, PageQueryDTO pageQuery, Long currentUserId);
    PageVO<UserVO> getMutualFollows(Long userId, PageQueryDTO pageQuery, Long currentUserId);
    boolean isFollowing(Long followerId, Long followeeId);
}
