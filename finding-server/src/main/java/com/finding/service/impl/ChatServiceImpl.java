package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.dto.MessageSendDTO;
import com.finding.entity.*;
import com.finding.config.RabbitMQConfig;
import com.finding.event.MsgSendMessageDTO;
import com.finding.mapper.*;
import com.finding.service.ChatService;
import com.finding.vo.ChatMessageVO;
import com.finding.vo.ConversationVO;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天服务 —— 基于 MallChat Room 模型重构。
 * 核心：room 作为统一聊天容器，private_chat 引用 room_id，contact 引用 room_id。
 * 流程: 获取/创建Room → 保存消息 → 发布 MessageSendEvent（异步推送）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final PrivateChatMapper privateChatMapper;
    private final RoomMapper roomMapper;
    private final RoomFriendMapper roomFriendMapper;
    private final ContactMapper contactMapper;
    private final UserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public ConversationVO getOrCreateConversation(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能和自己聊天");
        }

        long uid1 = Math.min(userId, targetUserId);
        long uid2 = Math.max(userId, targetUserId);
        String roomKey = uid1 + "_" + uid2;

        // 查找已有房间
        RoomFriend rf = roomFriendMapper.selectOne(
                new LambdaQueryWrapper<RoomFriend>()
                        .eq(RoomFriend::getRoomKey, roomKey));

        if (rf == null) {
            // 创建 Room
            Room room = new Room();
            room.setType(1); // 单聊
            room.setActiveTime(LocalDateTime.now());
            roomMapper.insert(room);

            // 创建 RoomFriend
            rf = new RoomFriend();
            rf.setRoomId(room.getId());
            rf.setUid1(uid1);
            rf.setUid2(uid2);
            rf.setRoomKey(roomKey);
            rf.setStatus(1); // normal
            roomFriendMapper.insert(rf);
        }

        // 确保双方都有 contact
        ensureContact(userId, rf.getRoomId());
        ensureContact(targetUserId, rf.getRoomId());

        // 构建返回
        return buildConvVO(rf.getRoomId(), userId, targetUserId);
    }

    @Override
    public List<ConversationVO> listConversations(Long userId) {
        // 从 contact 表查询会话列表（按活跃时间倒序）
        List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, userId)
                        .orderByDesc(Contact::getActiveTime));

        if (contacts.isEmpty()) return List.of();

        // 批量查询 room_friend 获取对方 UID
        List<Long> roomIds = contacts.stream().map(Contact::getRoomId).collect(Collectors.toList());
        List<RoomFriend> roomFriends = roomFriendMapper.selectList(
                new LambdaQueryWrapper<RoomFriend>().in(RoomFriend::getRoomId, roomIds));

        // 批量查询对方用户
        Set<Long> targetUids = new HashSet<>();
        Map<Long, Long> roomToTargetUser = new HashMap<>(); // roomId -> targetUserId
        for (RoomFriend rf : roomFriends) {
            Long targetUid = rf.getUid1().equals(userId) ? rf.getUid2() : rf.getUid1();
            targetUids.add(targetUid);
            roomToTargetUser.put(rf.getRoomId(), targetUid);
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!targetUids.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(targetUids);
            users.forEach(u -> userMap.put(u.getId(), u));
        }

        // 查询每个 room 的最后消息时间
        Map<Long, PrivateChat> lastMsgMap = new HashMap<>();
        for (Long roomId : roomIds) {
            List<PrivateChat> msgs = privateChatMapper.selectList(
                    new LambdaQueryWrapper<PrivateChat>()
                            .eq(PrivateChat::getRoomId, roomId)
                            .orderByDesc(PrivateChat::getCreatedAt)
                            .last("LIMIT 1"));
            if (!msgs.isEmpty()) lastMsgMap.put(roomId, msgs.get(0));
        }

        // 组装结果
        List<ConversationVO> result = new ArrayList<>();
        for (Contact contact : contacts) {
            Long targetUserId = roomToTargetUser.get(contact.getRoomId());
            if (targetUserId == null) continue;

            User target = userMap.get(targetUserId);
            ConversationVO vo = new ConversationVO();
            vo.setId(contact.getRoomId());
            vo.setRoomId(contact.getRoomId());
            vo.setTargetUserId(targetUserId);
            vo.setTargetNickname(target != null ? target.getNickname() : "用户" + targetUserId);
            vo.setTargetAvatar(target != null ? target.getAvatar() : null);
            vo.setLastMessageAt(contact.getActiveTime());

            PrivateChat lastMsg = lastMsgMap.get(contact.getRoomId());
            if (lastMsg != null) {
                vo.setLastMessage("image".equals(lastMsg.getMessageType()) ? "[图片]" : lastMsg.getContent());
            }

            // 计算未读数
            int unread = 0;
            if (lastMsg != null && contact.getReadTime() != null) {
                unread = Math.toIntExact(privateChatMapper.selectCount(
                        new LambdaQueryWrapper<PrivateChat>()
                                .eq(PrivateChat::getRoomId, contact.getRoomId())
                                .eq(PrivateChat::getToUserId, userId)
                                .eq(PrivateChat::getIsRead, 0)));
            } else if (lastMsg != null) {
                unread = Math.toIntExact(privateChatMapper.selectCount(
                        new LambdaQueryWrapper<PrivateChat>()
                                .eq(PrivateChat::getRoomId, contact.getRoomId())
                                .eq(PrivateChat::getToUserId, userId)
                                .eq(PrivateChat::getIsRead, 0)));
            }
            vo.setUnreadCount(unread);

            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public ConversationVO sendMessage(Long userId, MessageSendDTO dto) {
        // 获取或创建房间
        ConversationVO convVO = getOrCreateConversation(userId, dto.getToUserId());

        // 保存消息（使用 room_id）
        PrivateChat chat = new PrivateChat();
        chat.setConversationId(convVO.getRoomId()); // 兼容旧字段
        chat.setRoomId(convVO.getRoomId());
        chat.setFromUserId(userId);
        chat.setToUserId(dto.getToUserId());
        chat.setContent(dto.getContent());
        chat.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "text");
        chat.setIsRead(0);
        privateChatMapper.insert(chat);

        // 事务提交后再发送 MQ，确保消费者能读到消息
        final Long msgId = chat.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_SEND_MSG,
                        new MsgSendMessageDTO(msgId));
            }
        });

        return convVO;
    }

    @Override
    public PageVO<ChatMessageVO> getMessageHistory(Long userId, Long roomId, Long lastId, int size) {
        // 查询消息（按时间正序，最新的在后面）
        LambdaQueryWrapper<PrivateChat> wrapper = new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getRoomId, roomId)
                .orderByAsc(PrivateChat::getCreatedAt);
        if (lastId != null) wrapper.lt(PrivateChat::getId, lastId);

        Page<PrivateChat> page = new Page<>(1, size);
        Page<PrivateChat> result = privateChatMapper.selectPage(page, wrapper);

        // 标记已读
        markConversationRead(userId, roomId);

        // 映射为 ChatMessageVO
        List<ChatMessageVO> records = result.getRecords().stream()
                .map(m -> {
                    ChatMessageVO vo = new ChatMessageVO();
                    vo.setId(m.getId());
                    vo.setRoomId(m.getRoomId());
                    vo.setFromUserId(m.getFromUserId());
                    vo.setToUserId(m.getToUserId());
                    vo.setContent(m.getContent());
                    vo.setMessageType(m.getMessageType());
                    vo.setIsRead(m.getIsRead());
                    vo.setCreatedAt(m.getCreatedAt());
                    return vo;
                })
                .collect(Collectors.toList());

        return PageVO.of(records, result.getTotal(), 1, size);
    }

    @Override
    public void markConversationRead(Long userId, Long roomId) {
        // 将发给当前用户的消息标记为已读
        PrivateChat update = new PrivateChat();
        update.setIsRead(1);
        privateChatMapper.update(update,
                new LambdaQueryWrapper<PrivateChat>()
                        .eq(PrivateChat::getRoomId, roomId)
                        .eq(PrivateChat::getToUserId, userId)
                        .eq(PrivateChat::getIsRead, 0));

        // 更新 contact 已读时间
        Contact contact = contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, userId)
                        .eq(Contact::getRoomId, roomId));
        if (contact != null) {
            contact.setReadTime(LocalDateTime.now());
            contactMapper.updateById(contact);
        }
    }

    // ── Private helpers ──

    /** 确保用户在 room 中有 contact 记录 */
    private void ensureContact(Long uid, Long roomId) {
        Contact contact = contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, uid)
                        .eq(Contact::getRoomId, roomId));
        if (contact == null) {
            contact = new Contact();
            contact.setUid(uid);
            contact.setRoomId(roomId);
            contact.setActiveTime(LocalDateTime.now());
            contact.setReadTime(LocalDateTime.now());
            contactMapper.insert(contact);
        }
    }

    /** 构建会话 VO */
    private ConversationVO buildConvVO(Long roomId, Long currentUserId, Long targetUserId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(roomId);
        vo.setRoomId(roomId);
        vo.setTargetUserId(targetUserId);

        User target = userMapper.selectById(targetUserId);
        if (target != null) {
            vo.setTargetNickname(target.getNickname());
            vo.setTargetAvatar(target.getAvatar());
        }

        // 获取最后一条消息
        List<PrivateChat> lastMsgs = privateChatMapper.selectList(
                new LambdaQueryWrapper<PrivateChat>()
                        .eq(PrivateChat::getRoomId, roomId)
                        .orderByDesc(PrivateChat::getCreatedAt)
                        .last("LIMIT 1"));
        if (!lastMsgs.isEmpty()) {
            PrivateChat last = lastMsgs.get(0);
            vo.setLastMessage("image".equals(last.getMessageType()) ? "[图片]" : last.getContent());
            vo.setLastMessageAt(last.getCreatedAt());
        }

        return vo;
    }
}
