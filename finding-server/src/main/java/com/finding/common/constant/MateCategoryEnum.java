package com.finding.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MateCategoryEnum {

    TRAVEL("travel", "旅游搭子"),
    CARPOOL("carpool", "拼车搭子"),
    FITNESS("fitness", "健身搭子"),
    STUDY("study", "学习搭子"),
    EXAM("exam", "备考搭子"),
    SPORTS("sports", "运动搭子"),
    GAMING("gaming", "游戏搭子"),
    ENTERTAINMENT("entertainment", "娱乐搭子"),
    OTHER("other", "其他");

    private final String code;
    private final String desc;
}
