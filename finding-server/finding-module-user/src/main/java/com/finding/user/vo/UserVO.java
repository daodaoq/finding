package com.finding.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String nickname;
    private String avatar;
    /** 个人中心资料卡背景图 URL */
    private String profileBackground;
    private Integer gender;
    private String school;
    private String signature;
    private String city;
    private Integer realNameVerified;
    private Integer targetType;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    // Computed fields
    private Integer followerCount;
    private Integer followingCount;
    private Integer postCount;
    /** 互关(好友)数量:我关注且对方也关注我 */
    private Integer mutualCount;
    private Boolean isFollowed;
}
