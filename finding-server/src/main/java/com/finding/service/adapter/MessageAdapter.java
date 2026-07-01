package com.finding.service.adapter;

import com.finding.entity.PrivateChat;
import com.finding.entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息适配器 —— 将实体转换为前端需要的 VO/Map。
 * 参考 MallChat 的 MessageAdapter + WSAdapter 设计。
 */
public final class MessageAdapter {

    private MessageAdapter() {}

    /** 构建 WebSocket 推送消息 */
    public static Map<String, Object> buildWsMessage(PrivateChat chat, User fromUser) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "chat");
        msg.put("messageId", chat.getId());
        msg.put("fromUserId", chat.getFromUserId());
        msg.put("fromUserNickname", fromUser != null ? fromUser.getNickname() : "");
        msg.put("fromUserAvatar", fromUser != null ? fromUser.getAvatar() : "");
        msg.put("toUserId", chat.getToUserId());
        msg.put("conversationId", chat.getConversationId());
        msg.put("content", chat.getContent());
        msg.put("messageType", chat.getMessageType());
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    /** 构建系统消息 */
    public static Map<String, Object> buildSystemMessage(String content, Long toUserId) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "system");
        msg.put("content", content);
        msg.put("toUserId", toUserId);
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    /** 构建在线/离线通知 */
    public static Map<String, Object> buildOnlineNotify(Long userId, boolean online) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", online ? "online" : "offline");
        msg.put("userId", userId);
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }
}
