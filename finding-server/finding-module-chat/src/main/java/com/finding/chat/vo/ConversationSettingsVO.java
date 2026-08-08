package com.finding.chat.vo;

import lombok.Data;

/** 会话设置(置顶/免打扰/聊天背景) */
@Data
public class ConversationSettingsVO {

    private Long roomId;
    private Boolean pinned;
    private Boolean muted;
    private String background;
}
