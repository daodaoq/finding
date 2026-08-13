package com.finding.bridge.config;

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

    // ── 反馈闭环:根据用户申请/跳过历史调整 ──
    /** 我申请过的人多来自该校 → 同校候选加分 */
    private int likedSchool = 8;
    /** 我申请过的人多在该市 → 同城候选加分 */
    private int likedCity = 2;
    /** 我跳过的人来自该校 → 同校候选减分 */
    private int skippedSchool = 8;
    /** 我跳过的人在该市 → 同城候选减分 */
    private int skippedCity = 2;
    /** 冷启动:无历史反馈时,已认证候选加分 */
    private int coldStartVerified = 4;
    /** 冷启动:无历史反馈时,近期活跃候选加分 */
    private int coldStartActive = 3;
}
