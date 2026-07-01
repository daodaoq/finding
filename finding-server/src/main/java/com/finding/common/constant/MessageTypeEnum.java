package com.finding.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    LIKE("like", "点赞通知"),
    COMMENT("comment", "评论通知"),
    FOLLOW("follow", "关注通知"),
    MATE_REQUEST("mate_request", "搭子申请"),
    MATE_ACCEPTED("mate_accepted", "搭子申请通过"),
    MATE_REJECTED("mate_rejected", "搭子申请拒绝"),
    SYSTEM("system", "系统通知"),
    CHAT("chat", "私信消息");

    private final String code;
    private final String desc;
}
