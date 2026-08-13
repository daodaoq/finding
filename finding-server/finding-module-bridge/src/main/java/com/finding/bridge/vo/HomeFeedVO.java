package com.finding.bridge.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeFeedVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private Integer age;         // 有生日时计算
    private String school;
    private String signature;
    private String city;
    private Double distanceKm;
    private Integer verified;    // 是否已实名认证 0/1
    private Integer targetType;  // 交友目标 0=未设置 1=找对象 2=交朋友
    private LocalDateTime lastLoginAt;
    private Boolean online;      // 是否实时在线(Redis 心跳,受 showLastOnline 配置裁剪)

    // Computed
    private Boolean isLiked;    // 是否已发送聊天申请
    private Boolean liked;      // 是否已心动(双向 match 的喜欢)
    private Integer mutualFriends;
    /** 匹配理由,如「同校」「已认证」「兴趣相投」 */
    private List<String> matchReasons;
}
