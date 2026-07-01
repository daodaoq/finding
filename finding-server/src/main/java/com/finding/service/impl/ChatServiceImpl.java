package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.dto.MessageSendDTO;
import com.finding.entity.*;
import com.finding.event.MessageSendEvent;
import com.finding.mapper.*;
import com.finding.service.ChatService;
import com.finding.vo.ConversationVO;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天服务 —— 参考 MallChat ChatServiceImpl 架构。
 * 核心流程: 校验 → 创建/获取房间 → 保存消息 → 发布 MessageSendEvent（异步推送）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final PrivateChatMapper privateChatMapper;
    private final RoomMapper roomMapper;
    private final RoomFriendMapper roomFriendMapper;
    private final ContactMapper contactMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ConversationVO getOrCreateConversation(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能和自己聊天");
        }

        // 确保 user1 < user2
        long uid1 = Math.min(userId, targetUserId);
        long uid2 = Math.max(userId, targetUserId);

        Conversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUser1Id, uid1)
                        .eq(Conversation::getUser2Id, uid2));

        if (conv == null) {
            conv = new Conversation();
            conv.setUser1Id(uid1);
            conv.setUser2Id(uid2);
            conv.setLastMessageAt(LocalDateTime.now());
            conversationMapper.insert(conv);

            // 同时创建 Room + RoomFriend（对接 MallChat 房间体系）
            Room room = new Room();
            room.setType(1); // 单聊
            room.setActiveTime(LocalDateTime.now());
            roomMapper.insert(room);

            RoomFriend rf = new RoomFriend();
            rf.setRoomId(room.getId());
            rf.setUid1(uid1);
            rf.setUid2(uid2);
            rf.setRoomKey(uid1 + "_" + uid2);
            roomFriendMapper.insert(rf);
        }

        return toConversationVO(conv, userId);
    }

    @Override
    public List<ConversationVO> listConversations(Long userId) {
        // 从 contact 表查询用户的会话列表（按活跃时间排序）
        List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, userId)
                        .orderByDesc(Contact::getActiveTime));

        return contacts.stream().map(c -> {
            Conversation conv = conversationMapper.selectById(c.getRoomId());
            if (conv == null) return null;
            return toConversationVO(conv, userId);
        }).filter(v -> v != null).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationVO sendMessage(Long userId, MessageSendDTO dto) {
        // 获取或创建会话 + 房间
        ConversationVO convVO = getOrCreateConversation(userId, dto.getToUserId());

        // 保存消息
        PrivateChat chat = new PrivateChat();
        chat.setConversationId(convVO.getId());
        chat.setFromUserId(userId);
        chat.setToUserId(dto.getToUserId());
        chat.setContent(dto.getContent());
        chat.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "text");
        chat.setIsRead(0);
        privateChatMapper.insert(chat);

        // 更新会话的最后消息
        Conversation conv = conversationMapper.selectById(convVO.getId());
        conv.setLastMessage(dto.getContent());
        conv.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conv);

        // 发布消息发送事件 → 异步推送（替代直接调用 WebSocket）
        eventPublisher.publishEvent(new MessageSendEvent(this, chat.getId()));

        return convVO;
    }

    @Override
    public PageVO<ConversationVO> getMessageHistory(Long userId, Long conversationId, Long lastId, int size) {
        LambdaQueryWrapper<PrivateChat> wrapper = new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getConversationId, conversationId)
                .orderByDesc(PrivateChat::getCreatedAt);
        if (lastId != null) wrapper.lt(PrivateChat::getId, lastId);

        Page<PrivateChat> page = new Page<>(1, size);
        Page<PrivateChat> result = privateChatMapper.selectPage(page, wrapper);

        // 标记已读
        markConversationRead(userId, conversationId);

        return PageVO.of(List.of(), result.getTotal(), 1, size);
    }

    @Override
    public void markConversationRead(Long userId, Long conversationId) {
        PrivateChat update = new PrivateChat();
        update.setIsRead(1);
        privateChatMapper.update(update,
                new LambdaQueryWrapper<PrivateChat>()
                        .eq(PrivateChat::getConversationId, conversationId)
                        .eq(PrivateChat::getToUserId, userId)
                        .eq(PrivateChat::getIsRead, 0));

        // 更新 contact 已读时间
        Contact contact = contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, userId)
                        .eq(Contact::getRoomId, conversationId));
        if (contact != null) {
            contact.setReadTime(LocalDateTime.now());
            contactMapper.updateById(contact);
        }
    }

    private ConversationVO toConversationVO(Conversation conv, Long currentUserId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        Long targetId = conv.getUser1Id().equals(currentUserId) ? conv.getUser2Id() : conv.getUser1Id();
        vo.setTargetUserId(targetId);
        vo.setLastMessage(conv.getLastMessage());
        vo.setLastMessageAt(conv.getLastMessageAt());

        User target = userMapper.selectById(targetId);
        if (target != null) {
            vo.setTargetNickname(target.getNickname());
            vo.setTargetAvatar(target.getAvatar());
        }
        return vo;
    }
}
