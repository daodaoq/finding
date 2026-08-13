package com.finding.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心动/配对列表项 —— 展示对方基础信息 + 发生时间 + 是否已配对。
 */
@Data
public class MatchUserVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;       // 0=unknown 1=male 2=female
    private String school;
    private String signature;
    private Integer verified;     // 是否已实名认证 0/1
    private Integer targetType;   // 交友目标 0=未设置 1=找对象 2=交朋友
    private LocalDateTime time;   // 心动/配对时间
    private Boolean isMatched;    // 是否已互相喜欢(配对)
}
