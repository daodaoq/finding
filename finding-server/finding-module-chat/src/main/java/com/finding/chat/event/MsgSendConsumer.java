package com.finding.chat.event;

import com.finding.framework.config.RabbitMQConfig;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.Room;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RoomMapper;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
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
    private final UserSettingsMapper userSettingsMapper;
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

        // 3. WebSocket 实时推送:无论是否免打扰都推送消息数据(免打扰仅抑制声音/弹窗,由 muted 标记交给前端)
        if (webSocketServer.isOnline(chat.getToUserId())) {
            webSocketServer.sendToUser(chat.getToUserId(),
                    buildWsMessage(chat, roomId, isMuted(chat.getToUserId(), roomId)));
        }
        // 发送方其他设备也同步(发送端本机由 REST 回执 + 前端按 messageId 去重)
        if (webSocketServer.isOnline(chat.getFromUserId())) {
            webSocketServer.sendToUser(chat.getFromUserId(),
                    buildWsMessage(chat, roomId, isMuted(chat.getFromUserId(), roomId)));
        }

        log.debug("MQ 消息处理完成: msgId={}, roomId={}, from={}, to={}",
                chat.getId(), roomId, chat.getFromUserId(), chat.getToUserId());
    }

    /** 判断接收方是否对该会话开启了免打扰(未显式设置则继承全局默认) */
    private boolean isMuted(Long uid, Long roomId) {
        Contact contact = contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, uid)
                        .eq(Contact::getRoomId, roomId));
        if (contact != null && contact.getMuted() != null) {
            return contact.getMuted() == 1;
        }
        UserSettings s = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, uid));
        return s != null && s.getChatMuted() != null && s.getChatMuted() == 1;
    }

    /** 构建推送消息(MQ 重复投递时为幂等;muted 仅作通知强度标记) */
    private WsMessage buildWsMessage(PrivateChat chat, Long roomId, boolean muted) {
        WsMessage wsMsg = new WsMessage();
        wsMsg.setType("chat");
        wsMsg.setFromUserId(chat.getFromUserId());
        wsMsg.setToUserId(chat.getToUserId());
        wsMsg.setConversationId(roomId);
        wsMsg.setContent(chat.getContent());
        wsMsg.setMessageType(chat.getMessageType());
        wsMsg.setMessageId(chat.getId());
        wsMsg.setMuted(muted);
        wsMsg.setTimestamp(System.currentTimeMillis());
        return wsMsg;
    }

    /** 更新/创建 contact(uk_uid_room 唯一约束下幂等,并发重复插入忽略) */
    private void updateContact(Long uid, Long roomId, Long msgId) {
        Contact contact = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, uid)
                        .eq(Contact::getRoomId, roomId));
        if (contact == null) {
            try {
                contact = new Contact();
                contact.setUid(uid);
                contact.setRoomId(roomId);
                contact.setActiveTime(LocalDateTime.now());
                contact.setLastMsgId(msgId);
                contactMapper.insert(contact);
            } catch (DuplicateKeyException e) {
                contact = contactMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contact>()
                                .eq(Contact::getUid, uid)
                                .eq(Contact::getRoomId, roomId));
                if (contact != null) {
                    contact.setActiveTime(LocalDateTime.now());
                    contact.setLastMsgId(msgId);
                    contactMapper.updateById(contact);
                }
            }
        } else {
            contact.setActiveTime(LocalDateTime.now());
            contact.setLastMsgId(msgId);
            contactMapper.updateById(contact);
        }
    }
}
