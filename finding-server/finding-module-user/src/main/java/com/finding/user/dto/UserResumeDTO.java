package com.finding.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 情感简历保存/更新请求体(与 user_resume 表字段对应)。
 *
 * <p>字段级 Bean Validation 兜底;生日为年龄唯一来源,服务层会由 birthday 推导 age。
 * 所有字段可空(未填写则留空)。</p>
 */
@Data
public class UserResumeDTO {

    // ── 板块1 基础信息 ──
    @Min(value = 1, message = "gender 仅允许 1=男 2=女")
    @Max(value = 2, message = "gender 仅允许 1=男 2=女")
    private Integer gender;         // 1=男 2=女

    @Min(value = 0, message = "age 不能为负数")
    @Max(value = 120, message = "age 超出合理范围")
    private Integer age;

    @PastOrPresent(message = "birthday 不能是未来日期")
    private LocalDate birthday;

    @Size(max = 20, message = "constellation 长度不能超过 20")
    private String constellation;   // 星座

    @Min(value = 80, message = "身高超出合理范围(80-250cm)")
    @Max(value = 250, message = "身高超出合理范围(80-250cm)")
    private Integer heightCm;

    @Min(value = 20, message = "体重超出合理范围(20-300kg)")
    @Max(value = 300, message = "体重超出合理范围(20-300kg)")
    private Integer weightKg;

    @Size(max = 100, message = "campus 长度不能超过 100")
    private String campus;
    @Size(max = 100, message = "majorGrade 长度不能超过 100")
    private String majorGrade;
    @Size(max = 100, message = "hometown 长度不能超过 100")
    private String hometown;
    @Size(max = 100, message = "career 长度不能超过 100")
    private String career;
    @Size(max = 200, message = "dailyRoutine 长度不能超过 200")
    private String dailyRoutine;
    @Size(max = 200, message = "relationshipStatus 长度不能超过 200")
    private String relationshipStatus;
    @Size(max = 500, message = "coreBottomLine 长度不能超过 500")
    private String coreBottomLine;

    // ── 板块2 自我画像 ──
    @Size(max = 10, message = "mbti 长度不能超过 10")
    private String mbti;
    @Size(max = 500, message = "personalityTraits 长度不能超过 500")
    private String personalityTraits;
    @Size(max = 500, message = "inLoveLook 长度不能超过 500")
    private String inLoveLook;
    @Size(max = 500, message = "flaws 长度不能超过 500")
    private String flaws;
    @Size(max = 500, message = "worldview 长度不能超过 500")
    private String worldview;
    @Size(max = 500, message = "personalTags 长度不能超过 500")
    private String personalTags;

    // ── 板块3 恋爱复盘 ──
    @Size(max = 50, message = "relationshipCount 长度不能超过 50")
    private String relationshipCount;
    @Size(max = 500, message = "breakupReason 长度不能超过 500")
    private String breakupReason;
    @Size(max = 500, message = "loveShortcoming 长度不能超过 500")
    private String loveShortcoming;
    @Size(max = 500, message = "loveInsight 长度不能超过 500")
    private String loveInsight;
    @Size(max = 500, message = "loveGrowth 长度不能超过 500")
    private String loveGrowth;

    // ── 板块4 恋爱相处模式 ──
    @Size(max = 500, message = "dailyCompany 长度不能超过 500")
    private String dailyCompany;
    @Size(max = 500, message = "fightMode 长度不能超过 500")
    private String fightMode;
    @Size(max = 500, message = "loveExpression 长度不能超过 500")
    private String loveExpression;
    @Size(max = 500, message = "oppositeBoundary 长度不能超过 500")
    private String oppositeBoundary;

    // ── 板块5 个人生活与规划 ──
    @Size(max = 500, message = "dailyStatus 长度不能超过 500")
    private String dailyStatus;
    @Size(max = 500, message = "lifeHabits 长度不能超过 500")
    private String lifeHabits;
    @Size(max = 500, message = "shortTermPlan 长度不能超过 500")
    private String shortTermPlan;
    @Size(max = 500, message = "longTermPlan 长度不能超过 500")
    private String longTermPlan;
    @Size(max = 500, message = "hobbies 长度不能超过 500")
    private String hobbies;
    @Size(max = 500, message = "marriagePlan 长度不能超过 500")
    private String marriagePlan;

    // ── 板块6 理想的另一半 ──
    @Size(max = 500, message = "hardConditions 长度不能超过 500")
    private String hardConditions;
    @Size(max = 500, message = "softExpectations 长度不能超过 500")
    private String softExpectations;

    // ── 板块7 加分项 ──
    @Size(max = 500, message = "bonusPoints 长度不能超过 500")
    private String bonusPoints;

    // ── 板块8 走心宣言 ──
    @Size(max = 500, message = "loveExpectation 长度不能超过 500")
    private String loveExpectation;
    @Size(max = 500, message = "loveAttitude 长度不能超过 500")
    private String loveAttitude;

    // ── 板块8 生活相册(图片URL数组,最多9张) ──
    @Size(max = 9, message = "相册最多 9 张")
    private List<String> photoAlbum;
}
