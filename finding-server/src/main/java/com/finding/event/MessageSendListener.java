package com.finding.event;

import com.finding.entity.*;
import com.finding.mapper.*;
import com.finding.websocket.WebSocketServer;
import com.finding.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消息发送异步监听器 —— 等价于 MallChat 的 MsgSendConsumer (RocketMQ)。
 * 使用 Spring @Async + @EventListener 替代 MQ，轻量且解耦。
 * 负责：更新房间活跃时间 → 更新会话 → WebSocket 推送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendListener {

    private final PrivateChatMapper privateChatMapper;
    private final RoomMapper roomMapper;
    private final RoomFriendMapper roomFriendMapper;
    private final ContactMapper contactMapper;
    private final WebSocketServer webSocketServer;

    @Async
    @EventListener
    public void handleMessageSend(MessageSendEvent event) {
        PrivateChat chat = privateChatMapper.selectById(event.getMsgId());
        if (chat == null) return;

        // 1. 更新房间活跃时间
        Room room = roomMapper.selectById(chat.getConversationId());
        if (room != null) {
            room.setActiveTime(LocalDateTime.now());
            room.setLastMsgId(chat.getId());
            roomMapper.updateById(room);
        }

        // 2. 更新双方的会话（contact）
        updateContact(chat.getFromUserId(), chat.getConversationId(), chat.getId());
        updateContact(chat.getToUserId(), chat.getConversationId(), chat.getId());

        // 3. WebSocket 实时推送给接收者
        if (webSocketServer.isOnline(chat.getToUserId())) {
            WsMessage wsMsg = new WsMessage();
            wsMsg.setType("chat");
            wsMsg.setFromUserId(chat.getFromUserId());
            wsMsg.setToUserId(chat.getToUserId());
            wsMsg.setConversationId(chat.getConversationId());
            wsMsg.setContent(chat.getContent());
            wsMsg.setMessageType(chat.getMessageType());
            wsMsg.setMessageId(chat.getId());
            wsMsg.setTimestamp(System.currentTimeMillis());
            webSocketServer.sendToUser(chat.getToUserId(), wsMsg);
        }

        log.debug("消息推送完成: msgId={}, from={}, to={}", chat.getId(), chat.getFromUserId(), chat.getToUserId());
    }

    private void updateContact(Long uid, Long roomId, Long msgId) {
        Contact contact = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, uid)
                        .eq(Contact::getRoomId, roomId));
        if (contact == null) {
            contact = new Contact();
            contact.setUid(uid);
            contact.setRoomId(roomId);
            contact.setActiveTime(LocalDateTime.now());
            contact.setLastMsgId(msgId);
            contactMapper.insert(contact);
        } else {
            contact.setActiveTime(LocalDateTime.now());
            contact.setLastMsgId(msgId);
            contactMapper.updateById(contact);
        }
    }
}
