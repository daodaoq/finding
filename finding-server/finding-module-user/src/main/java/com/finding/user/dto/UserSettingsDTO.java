package com.finding.user.dto;

import lombok.Data;

/**
 * 用户全局设置更新入参 —— 传 null 表示不修改该字段。
 */
@Data
public class UserSettingsDTO {

    /** 全局默认聊天背景;传空串=清除(恢复默认) */
    private String chatBg;
    private Integer chatMuted;
    private Integer friendAddMode;
    private Integer profileVisible;
    private Integer searchable;
}
