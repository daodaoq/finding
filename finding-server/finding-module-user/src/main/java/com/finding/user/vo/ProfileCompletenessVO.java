package com.finding.user.vo;

import lombok.Data;

import java.util.List;

/** 资料完整度(0-10)+ 缺失项,用于「还缺 xxx」自我引导 */
@Data
public class ProfileCompletenessVO {

    /** 完整度评分 0-10 */
    private int score;
    /** 已填写项数 */
    private int filled;
    /** 总项数 */
    private int total;
    /** 缺失项中文标签(如 学校/城市/个性签名) */
    private List<String> missing;
}
