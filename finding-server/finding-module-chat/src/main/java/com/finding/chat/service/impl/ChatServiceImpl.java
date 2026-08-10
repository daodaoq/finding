package com.finding.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.chat.dto.MessageSendDTO;

import com.finding.framework.config.RabbitMQConfig;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import com.finding.chat.event.MsgSendMessageDTO;

import com.finding.chat.service.ChatService;
import com.finding.chat.vo.ChatMessageVO;
import com.finding.chat.vo.ConversationSettingsVO;
import com.finding.message.vo.ConversationVO;
import com.finding.common.PageVO;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.Report;
import com.finding.chat.entity.Room;
import com.finding.chat.entity.RoomFriend;
import com.finding.user.entity.User;
import com.finding.user.entity.UserSettings;
import com.finding.user.service.UserRelationshipService;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.ReportMapper;
import com.finding.chat.mapper.RoomFriendMapper;
import com.finding.chat.mapper.RoomMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;

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
    private final ReportMapper reportMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final RabbitTemplate rabbitTemplate;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final UserRelationshipService relationshipService;
    private final WebSocketServer webSocketServer;

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
            // 无既有会话 → 新建私聊:与被拉黑者不能建立新会话
            if (relationshipService.isBlockedEitherWay(userId, targetUserId)) {
                throw new BusinessException(ResultCode.RELATION_BLOCKED);
            }
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
        // 从 contact 表查询会话列表（置顶优先，再按活跃时间倒序）
        List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, userId)
                        .orderByDesc(Contact::getPinned)
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

        // 查询每个 room 的最后消息时间(仅当前用户这一侧可见的消息)
        Map<Long, PrivateChat> lastMsgMap = new HashMap<>();
        for (Long roomId : roomIds) {
            LambdaQueryWrapper<PrivateChat> lastWrapper = new LambdaQueryWrapper<PrivateChat>()
                    .eq(PrivateChat::getRoomId, roomId);
            applySideFilter(lastWrapper, userId, roomToTargetUser.get(roomId));
            List<PrivateChat> msgs = privateChatMapper.selectList(lastWrapper
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
            vo.setPinned(contact.getPinned() != null && contact.getPinned() == 1);
            Integer m = contact.getMuted();
            vo.setMuted(m != null ? m == 1 : globalMuted(userId));

            PrivateChat lastMsg = lastMsgMap.get(contact.getRoomId());
            if (lastMsg != null) {
                vo.setLastMessage("image".equals(lastMsg.getMessageType()) ? "[图片]" : lastMsg.getContent());
            }

            // 计算未读数(同样只统计我这一侧可见的未读)
            LambdaQueryWrapper<PrivateChat> unreadWrapper = new LambdaQueryWrapper<PrivateChat>()
                    .eq(PrivateChat::getRoomId, contact.getRoomId())
                    .eq(PrivateChat::getToUserId, userId)
                    .eq(PrivateChat::getIsRead, 0);
            applySideFilter(unreadWrapper, userId, targetUserId);
            vo.setUnreadCount(Math.toIntExact(privateChatMapper.selectCount(unreadWrapper)));

            result.add(vo);
        }
        return result;
    }

    /**
     * 单侧清空过滤: 只在当前用户这一侧未隐藏的消息上生效。
     * room_friend 约定 uid1 = 较小的用户ID, uid2 = 较大的用户ID。
     */
    private void applySideFilter(LambdaQueryWrapper<PrivateChat> wrapper, Long userId, Long targetUserId) {
        if (targetUserId == null) return;
        if (userId < targetUserId) {
            wrapper.eq(PrivateChat::getUid1Hidden, 0);
        } else {
            wrapper.eq(PrivateChat::getUid2Hidden, 0);
        }
    }

    @Override
    @Transactional
    public ConversationVO sendMessage(Long userId, MessageSendDTO dto) {
        // 拉黑拦截:任一方拉黑对方都禁止私聊
        if (relationshipService.isBlockedEitherWay(userId, dto.getToUserId())) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }
        // 获取或创建房间
        ConversationVO convVO = getOrCreateConversation(userId, dto.getToUserId());

        // XSS 清洗 + 违禁词拦截
        dto.setContent(XssUtil.clean(dto.getContent()));
        sensitiveWordFilter.assertClean(dto.getContent());

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
        // 查询消息（按时间正序，最新的在后面），只返回我这一侧未隐藏的（单侧清空）
        RoomFriend rf = roomFriendMapper.selectOne(new LambdaQueryWrapper<RoomFriend>()
                .eq(RoomFriend::getRoomId, roomId));
        LambdaQueryWrapper<PrivateChat> wrapper = new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getRoomId, roomId);
        if (rf != null) {
            if (rf.getUid1().equals(userId)) wrapper.eq(PrivateChat::getUid1Hidden, 0);
            else wrapper.eq(PrivateChat::getUid2Hidden, 0);
        }
        wrapper.orderByAsc(PrivateChat::getCreatedAt);
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
                    vo.setIsRecalled(m.getIsRecalled());
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

    @Override
    public ConversationSettingsVO getConversationSettings(Long userId, Long roomId) {
        ConversationSettingsVO vo = new ConversationSettingsVO();
        vo.setRoomId(roomId);
        Contact contact = findContact(userId, roomId);
        if (contact == null) {
            vo.setPinned(false);
            vo.setMuted(globalMuted(userId));
            vo.setBackground(globalBackground(userId));
            return vo;
        }
        vo.setPinned(contact.getPinned() != null && contact.getPinned() == 1);
        Integer muted = contact.getMuted();
        vo.setMuted(muted != null ? muted == 1 : globalMuted(userId));
        String bg = contact.getBackground();
        vo.setBackground(bg != null ? bg : globalBackground(userId));
        return vo;
    }

    @Override
    public void updateConversationSettings(Long userId, Long roomId, Boolean pinned, Boolean muted, String background) {
        Contact contact = findContact(userId, roomId);
        if (contact == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话不存在");
        }
        // 用 LambdaUpdateWrapper.set 显式写列:updateById 默认忽略 null 字段,
        // 背景"恢复默认"需把 background 置 null,必须显式 set
        LambdaUpdateWrapper<Contact> wrapper = new LambdaUpdateWrapper<Contact>()
                .eq(Contact::getId, contact.getId());
        if (pinned != null) wrapper.set(Contact::getPinned, pinned ? 1 : 0);
        if (muted != null) wrapper.set(Contact::getMuted, muted ? 1 : 0);
        if (background != null) wrapper.set(Contact::getBackground, background.isEmpty() ? null : background);
        contactMapper.update(null, wrapper);
    }

    @Override
    public PageVO<ChatMessageVO> searchMessages(Long userId, Long roomId, String keyword, int size) {
        // 校验用户在该会话中
        findContact(userId, roomId);

        // 只搜索我这一侧未隐藏的消息(单侧清空后不再出现在搜索结果)
        RoomFriend rf = roomFriendMapper.selectOne(new LambdaQueryWrapper<RoomFriend>()
                .eq(RoomFriend::getRoomId, roomId));
        LambdaQueryWrapper<PrivateChat> wrapper = new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getRoomId, roomId)
                .like(StringUtils.hasText(keyword), PrivateChat::getContent, keyword);
        if (rf != null) {
            if (rf.getUid1().equals(userId)) wrapper.eq(PrivateChat::getUid1Hidden, 0);
            else wrapper.eq(PrivateChat::getUid2Hidden, 0);
        }
        Page<PrivateChat> page = new Page<>(1, Math.min(size, 100));
        Page<PrivateChat> result = privateChatMapper.selectPage(page, wrapper.orderByDesc(PrivateChat::getCreatedAt));

        List<ChatMessageVO> records = result.getRecords().stream()
                .map(m -> {
                    ChatMessageVO vo = new ChatMessageVO();
                    vo.setId(m.getId());
                    vo.setRoomId(m.getRoomId());
                    vo.setFromUserId(m.getFromUserId());
                    vo.setToUserId(m.getToUserId());
                    vo.setContent(m.getContent());
                    vo.setMessageType(m.getMessageType());
                    vo.setIsRecalled(m.getIsRecalled());
                    vo.setIsRead(m.getIsRead());
                    vo.setCreatedAt(m.getCreatedAt());
                    return vo;
                })
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), 1, size);
    }

    @Override
    @Transactional
    public void clearMessages(Long userId, Long roomId) {
        if (findContact(userId, roomId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话不存在");
        }
        RoomFriend rf = roomFriendMapper.selectOne(new LambdaQueryWrapper<RoomFriend>()
                .eq(RoomFriend::getRoomId, roomId));
        if (rf == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话不存在");
        }
        // 只把"我"这一侧的消息标记为已清空,对方视角不受影响
        PrivateChat update = new PrivateChat();
        if (rf.getUid1().equals(userId)) {
            update.setUid1Hidden(1);
        } else if (rf.getUid2().equals(userId)) {
            update.setUid2Hidden(1);
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权操作该会话");
        }
        privateChatMapper.update(update, new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getRoomId, roomId));
    }

    @Override
    public void reportUser(Long fromUserId, Long toUserId, Long roomId, String reason) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能投诉自己");
        }
        if (userMapper.selectById(toUserId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        Report report = new Report();
        report.setFromUserId(fromUserId);
        report.setTargetUserId(toUserId);
        report.setRoomId(roomId);
        report.setReason(reason);
        report.setStatus(0);
        reportMapper.insert(report);
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        PrivateChat chat = privateChatMapper.selectById(messageId);
        if (chat == null || (chat.getIsRecalled() != null && chat.getIsRecalled() == 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息不存在或已撤回");
        }
        if (!chat.getFromUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能撤回自己发送的消息");
        }
        if (chat.getCreatedAt() != null
                && chat.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "发送超过 2 分钟的消息无法撤回");
        }
        PrivateChat update = new PrivateChat();
        update.setId(messageId);
        update.setIsRecalled(1);
        // 保留原 content,仅标记撤回,便于管理员审计原文
        privateChatMapper.updateById(update);

        // WS 通知双方刷新该消息
        WsMessage ws = new WsMessage();
        ws.setType("message_recalled");
        ws.setAction("private");
        ws.setMessageId(messageId);
        ws.setConversationId(chat.getRoomId());
        ws.setFromUserId(chat.getFromUserId());
        ws.setToUserId(chat.getToUserId());
        ws.setTimestamp(System.currentTimeMillis());
        webSocketServer.sendToUser(chat.getFromUserId(), ws);
        webSocketServer.sendToUser(chat.getToUserId(), ws);
    }

    private Contact findContact(Long uid, Long roomId) {
        return contactMapper.selectOne(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getUid, uid)
                .eq(Contact::getRoomId, roomId));
    }

    /** 读取用户全局默认免打扰(单个聊天未显式设置时继承) */
    private boolean globalMuted(Long userId) {
        UserSettings s = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId));
        return s != null && s.getChatMuted() != null && s.getChatMuted() == 1;
    }

    /** 读取用户全局默认聊天背景(单个聊天未设置时继承) */
    private String globalBackground(Long userId) {
        UserSettings s = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId));
        return s != null ? s.getChatBg() : null;
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
