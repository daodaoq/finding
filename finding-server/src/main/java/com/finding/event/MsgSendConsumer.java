package com.finding.event;

import com.finding.config.RabbitMQConfig;
import com.finding.entity.Contact;
import com.finding.entity.PrivateChat;
import com.finding.entity.Room;
import com.finding.mapper.ContactMapper;
import com.finding.mapper.PrivateChatMapper;
import com.finding.mapper.RoomMapper;
import com.finding.websocket.WebSocketServer;
import com.finding.websocket.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消息发送消费者 —— 参考 MallChat MsgSendConsumer。
 * 从 RabbitMQ 消费 chat.send.msg，负责：
 * 1. 更新 Room 活跃时间
 * 2. 更新双方 Contact（会话列表）
 * 3. WebSocket 推送给接收方
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MsgSendConsumer {

    private final PrivateChatMapper privateChatMapper;
    private final RoomMapper roomMapper;
    private final ContactMapper contactMapper;
    private final WebSocketServer webSocketServer;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SEND_MSG)
    public void handleMsgSend(MsgSendMessageDTO dto) {
        PrivateChat chat = privateChatMapper.selectById(dto.getMsgId());
        if (chat == null) {
            log.warn("MsgSendConsumer: 消息不存在, msgId={}", dto.getMsgId());
            return;
        }

        Long roomId = chat.getRoomId();
        if (roomId == null) {
            log.warn("MsgSendConsumer: room_id 为空, msgId={}", chat.getId());
            return;
        }

        // 1. 更新 Room 活跃时间
        Room room = roomMapper.selectById(roomId);
        if (room != null) {
            room.setActiveTime(LocalDateTime.now());
            room.setLastMsgId(chat.getId());
            roomMapper.updateById(room);
        }

        // 2. 更新双方 Contact（会话列表）
        updateContact(chat.getFromUserId(), roomId, chat.getId());
        updateContact(chat.getToUserId(), roomId, chat.getId());

        // 3. WebSocket 实时推送给接收方
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

        log.debug("MQ 消息处理完成: msgId={}, roomId={}, from={}, to={}",
                chat.getId(), roomId, chat.getFromUserId(), chat.getToUserId());
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
