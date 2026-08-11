package com.finding.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    BANNED(0, "已禁用"),
    ACTIVE(1, "正常"),
    FROZEN(2, "已冻结"),
    DELETED(3, "已注销");

    private final int code;
    private final String desc;
}
