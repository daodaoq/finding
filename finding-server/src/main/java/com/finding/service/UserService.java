package com.finding.service;

import com.finding.dto.PageQueryDTO;
import com.finding.vo.PageVO;
import com.finding.vo.UserVO;

public interface UserService {

    UserVO getUserProfile(Long userId, Long currentUserId);
    PageVO<UserVO> searchUsers(String keyword, PageQueryDTO pageQuery);
    void followUser(Long followerId, Long followeeId);
    void unfollowUser(Long followerId, Long followeeId);
    PageVO<UserVO> getFollowers(Long userId, PageQueryDTO pageQuery);
    PageVO<UserVO> getFollowing(Long userId, PageQueryDTO pageQuery);
    boolean isFollowing(Long followerId, Long followeeId);
}
