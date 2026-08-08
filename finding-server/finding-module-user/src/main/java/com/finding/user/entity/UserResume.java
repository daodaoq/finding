package com.finding.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 情感简历 —— 每位用户一份,默认对其他用户隐藏,互换信息后互相可见。
 */
@Data
@TableName(value = "user_resume", autoResultMap = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResume {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;

    // ── 板块1 基础信息 ──
    private Integer gender;         // 1=男 2=女
    private Integer age;
    private LocalDate birthday;
    private String constellation;   // 星座
    private Integer heightCm;       // 身高(cm)
    private Integer weightKg;       // 体重(kg)
    private String campus;          // 校区
    private String majorGrade;      // 专业年级
    private String hometown;        // 城市/家乡
    private String career;          // 职业
    private String dailyRoutine;    // 日常作息
    private String relationshipStatus; // 恋爱状态(单身时长/恋爱期待)
    private String coreBottomLine;  // 择偶核心底线

    // ── 板块2 自我画像 ──
    private String mbti;            // MBTI 人格
    private String personalityTraits; // 性格优点
    private String inLoveLook;      // 恋爱中的样子
    private String flaws;           // 小缺点
    private String worldview;       // 个人三观
    private String personalTags;    // 个人标签

    // ── 板块3 恋爱复盘 ──
    private String relationshipCount; // 恋爱次数
    private String breakupReason;     // 分手核心原因
    private String loveShortcoming;   // 恋爱短板
    private String loveInsight;       // 从前感情里学到的东西
    private String loveGrowth;        // 自己在感情里的成长/改掉的毛病

    // ── 板块4 恋爱相处模式 ──
    private String dailyCompany;      // 日常陪伴
    private String fightMode;         // 吵架模式
    private String loveExpression;    // 表达爱意方式
    private String oppositeBoundary;  // 与异性边界

    // ── 板块5 个人生活与规划 ──
    private String dailyStatus;       // 日常状态
    private String lifeHabits;        // 生活习惯
    private String shortTermPlan;     // 短期规划
    private String longTermPlan;      // 长期规划
    private String hobbies;           // 爱好与日常
    private String marriagePlan;      // 长期婚恋规划

    // ── 板块6 理想的另一半 ──
    private String hardConditions;    // 硬性条件
    private String softExpectations;  // 软性期待

    // ── 板块7 加分项(我能为恋爱带来什么) ──
    private String bonusPoints;

    // ── 板块8 走心宣言 ──
    private String loveExpectation;   // 对爱情的期待
    private String loveAttitude;      // 对新恋情的态度及承诺

    // ── 板块8 生活相册(图片URL数组) ──
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> photoAlbum;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
