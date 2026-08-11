package com.finding.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 相亲匹配评分权重 —— 通过 application.yml 的 finding.recommend.* 配置化,集中管理。
 */
@Data
@Component
@ConfigurationProperties(prefix = "finding.recommend")
public class MatchScoreWeights {

    /** 同校 */
    private int sameSchool = 15;
    /** 同城 */
    private int sameCity = 3;
    /** 已认证 */
    private int verified = 5;
    /** 24h 内活跃 */
    private int recentActive = 3;
    /** 有头像 */
    private int hasAvatar = 2;
    /** 兴趣关键词命中每个 */
    private int interestPerKeyword = 2;
    /** 距离 < 50km */
    private int distanceClose = 4;
    /** 资料完整度系数(完整度 0-10 × 系数) */
    private int completeness = 1;
}
