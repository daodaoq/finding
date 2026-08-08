package com.finding.chat.vo;

import lombok.Data;

/**
 * 聊天框「互换信息」按钮状态。
 * status: none / pendingSent / pendingReceived / approved / rejected
 */
@Data
public class InfoShareStatusVO {

    private String status;
    private Long shareId;
    private Long otherUserId;
    private String otherNickname;
    private String otherAvatar;
}
