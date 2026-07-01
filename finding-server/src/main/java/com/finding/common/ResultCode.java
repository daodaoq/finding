package com.finding.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    // Success
    SUCCESS(200, "操作成功"),

    // Auth errors (1xxx)
    UNAUTHORIZED(1001, "未登录或登录已过期"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    TOKEN_INVALID(1003, "Token无效"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    SMS_CODE_ERROR(1005, "验证码错误"),
    SMS_CODE_EXPIRED(1006, "验证码已过期"),
    SMS_SEND_TOO_FREQUENT(1007, "验证码发送过于频繁，请稍后再试"),
    ACCOUNT_DISABLED(1008, "账号已被禁用"),

    // User errors (2xxx)
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户名或手机号已注册"),
    REAL_NAME_NOT_VERIFIED(2003, "请先完成学生实名认证"),
    VERIFICATION_PENDING(2004, "实名认证审核中"),
    VERIFICATION_REJECTED(2005, "实名认证未通过"),
    ALREADY_FOLLOWED(2006, "已关注该用户"),
    NOT_FOLLOWED(2007, "未关注该用户"),
    CANNOT_FOLLOW_SELF(2008, "不能关注自己"),

    // Post errors (3xxx)
    POST_NOT_FOUND(3001, "动态不存在"),
    POST_DELETED(3002, "动态已删除"),
    POST_CONTENT_EMPTY(3003, "动态内容不能为空"),
    COMMENT_NOT_FOUND(3004, "评论不存在"),
    ALREADY_LIKED(3005, "已经点过赞了"),

    // Mate errors (4xxx)
    MATE_NOT_FOUND(4001, "搭子邀约不存在"),
    MATE_CLOSED(4002, "搭子邀约已关闭"),
    MATE_FULL(4003, "搭子邀约人数已满"),
    ALREADY_JOINED(4004, "已申请加入该邀约"),
    NOT_CREATOR(4005, "只有发起者可以执行此操作"),
    JOIN_REQUEST_NOT_FOUND(4006, "申请记录不存在"),

    // Message errors (5xxx)
    MESSAGE_NOT_FOUND(5001, "消息不存在"),
    CONVERSATION_NOT_FOUND(5002, "会话不存在"),
    CHAT_LIMIT_EXCEEDED(5003, "今日私信次数已达上限，请完成实名认证"),

    // Common errors (9xxx)
    PARAM_ERROR(9001, "参数错误"),
    PARAM_VALIDATION_FAILED(9002, "参数校验失败"),
    INTERNAL_ERROR(9999, "服务器内部错误");

    private final int code;
    private final String message;
}
