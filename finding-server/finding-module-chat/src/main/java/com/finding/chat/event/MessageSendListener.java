package com.finding.chat.event;



import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.Room;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RoomFriendMapper;
import com.finding.chat.mapper.RoomMapper;

/**
 * 消息发送异步监听器（已废弃，保留作为参考）。
 * 现由 RabbitMQ 的 MsgSendConsumer 替代。
 *
 * @deprecated 使用 {@link MsgSendConsumer}（RabbitMQ 消费者）。
 * @see MsgSendConsumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated
public class MessageSendListener {

    private final PrivateChatMapper privateChatMapper;
    private final RoomMapper roomMapper;
    private final RoomFriendMapper roomFriendMapper;
    private final ContactMapper contactMapper;
    private final WebSocketServer webSocketServer;

    // @Async
    // @EventListener  // 已禁用，由 MsgSendConsumer 替代
    public void handleMessageSend(MessageSendEvent event) {
        PrivateChat chat = privateChatMapper.selectById(event.getMsgId());
        if (chat == null) return;

        Long roomId = chat.getRoomId();
        if (roomId == null) {
            log.warn("消息缺少 room_id, msgId={}", chat.getId());
            return;
        }

        // 1. 更新房间活跃时间
        Room room = roomMapper.selectById(roomId);
        if (room != null) {
            room.setActiveTime(LocalDateTime.now());
            room.setLastMsgId(chat.getId());
            roomMapper.updateById(room);
        }

        // 2. 更新双方的会话（contact）
        updateContact(chat.getFromUserId(), roomId, chat.getId());
        updateContact(chat.getToUserId(), roomId, chat.getId());

        // 3. WebSocket 实时推送给接收者
        if (webSocketServer.isOnline(chat.getToUserId())) {
            WsMessage wsMsg = new WsMessage();
            wsMsg.setType("chat");
            wsMsg.setFromUserId(chat.getFromUserId());
            wsMsg.setToUserId(chat.getToUserId());
            wsMsg.setConversationId(roomId);
            wsMsg.setContent(chat.getContent());
            wsMsg.setMessageType(chat.getMessageType());
            wsMsg.setMessageId(chat.getId());
            wsMsg.setTimestamp(System.currentTimeMillis());
            webSocketServer.sendToUser(chat.getToUserId(), wsMsg);
        }

        log.debug("消息推送完成: msgId={}, roomId={}, from={}, to={}", chat.getId(), roomId, chat.getFromUserId(), chat.getToUserId());
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
