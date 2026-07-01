package com.finding.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HomeFeedVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String school;
    private String signature;
    private String city;
    private Double distanceKm;
    private LocalDateTime lastLoginAt;

    // Computed
    private Boolean isLiked;
    private Integer mutualFriends;
}
