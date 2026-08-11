package com.finding.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    /** 匹配理由,如「同校」「已认证」「兴趣相投」 */
    private List<String> matchReasons;
}
