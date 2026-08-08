package com.finding.chat.dto;

import lombok.Data;

/** 会话设置更新请求体 */
@Data
public class ConversationSettingsDTO {

    /** 是否置顶(null=不修改) */
    private Boolean pinned;

    /** 是否免打扰(null=不修改) */
    private Boolean muted;

    /** 聊天背景(null=不修改) */
    private String background;
}
