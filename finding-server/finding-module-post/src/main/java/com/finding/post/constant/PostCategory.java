package com.finding.post.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子分类(校园论坛固定分类)。
 * 用于发帖时的分类选择、列表/搜索的分类过滤与展示。
 */
@Getter
public enum PostCategory {

    STUDY("study", "学习交流"),
    LIFE("life", "校园生活"),
    CONFESSION("confession", "表白墙"),
    LOST_FOUND("lostfound", "失物招领"),
    JOB("job", "求职招聘"),
    FOOD("food", "美食探店"),
    SPORTS("sports", "运动健身"),
    OTHER("other", "其他");

    private final String code;
    private final String desc;

    PostCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 合法分类 code 集合(校验用) */
    public static final Set<String> SUPPORTED =
            Arrays.stream(values()).map(PostCategory::getCode).collect(Collectors.toSet());

    /** code → 中文描述(未知/空返回空串) */
    public static String descOf(String code) {
        if (code == null) return "";
        return Arrays.stream(values())
                .filter(c -> c.code.equals(code))
                .findFirst()
                .map(PostCategory::getDesc)
                .orElse("");
    }
}
