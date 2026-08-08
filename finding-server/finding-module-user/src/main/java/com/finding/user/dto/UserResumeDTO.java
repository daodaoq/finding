package com.finding.user.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 情感简历保存/更新请求体(与 user_resume 表字段对应)。
 */
@Data
public class UserResumeDTO {

    // ── 板块1 基础信息 ──
    private Integer gender;         // 1=男 2=女
    private Integer age;
    private LocalDate birthday;
    private String constellation;   // 星座
    private Integer heightCm;
    private Integer weightKg;
    private String campus;
    private String majorGrade;
    private String hometown;
    private String career;
    private String dailyRoutine;
    private String relationshipStatus;
    private String coreBottomLine;

    // ── 板块2 自我画像 ──
    private String mbti;
    private String personalityTraits;
    private String inLoveLook;
    private String flaws;
    private String worldview;
    private String personalTags;

    // ── 板块3 恋爱复盘 ──
    private String relationshipCount;
    private String breakupReason;
    private String loveShortcoming;
    private String loveInsight;
    private String loveGrowth;

    // ── 板块4 恋爱相处模式 ──
    private String dailyCompany;
    private String fightMode;
    private String loveExpression;
    private String oppositeBoundary;

    // ── 板块5 个人生活与规划 ──
    private String dailyStatus;
    private String lifeHabits;
    private String shortTermPlan;
    private String longTermPlan;
    private String hobbies;
    private String marriagePlan;

    // ── 板块6 理想的另一半 ──
    private String hardConditions;
    private String softExpectations;

    // ── 板块7 加分项 ──
    private String bonusPoints;

    // ── 板块8 走心宣言 ──
    private String loveExpectation;
    private String loveAttitude;

    // ── 板块8 生活相册 ──
    private List<String> photoAlbum;
}
