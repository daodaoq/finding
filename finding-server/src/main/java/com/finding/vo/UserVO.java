package com.finding.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String school;
    private String signature;
    private String city;
    private Integer realNameVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    // Computed fields
    private Integer followerCount;
    private Integer followingCount;
    private Integer postCount;
    private Boolean isFollowed;
}
