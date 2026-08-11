package com.finding.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.chat.dto.MessageSendDTO;

import com.finding.framework.util.InMemoryRateLimiter;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;

import com.finding.chat.service.ChatService;
import com.finding.chat.vo.ChatMessageVO;
import com.finding.chat.vo.ConversationSettingsVO;
import com.finding.message.vo.ConversationVO;
import com.finding.common.PageVO;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.finding.chat.entity.ChatOutbox;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.Report;
import com.finding.chat.entity.Room;
import com.finding.chat.entity.RoomFriend;
import com.finding.user.entity.User;
import com.finding.user.entity.UserSettings;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserWriteGuard;
import com.finding.chat.mapper.ChatOutboxMapper;
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
    private final ChatOutboxMapper chatOutboxMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final UserRelationshipService relationshipService;
    private final UserWriteGuard userWriteGuard;
    private final InMemoryRateLimiter rateLimiter;
    private final WebSocketServer webSocketServer;

    @Override
    public ConversationVO getConversation(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能和自己聊天");
        }

        long uid1 = Math.min(userId, targetUserId);
        long uid2 = Math.max(userId, targetUserId);
        String roomKey = uid1 + "_" + uid2;

        // 只查找已有会话,绝不创建:新会话只能由聊天申请批准流程(createConversation)建立
        RoomFriend rf = roomFriendMapper.selectOne(
                new LambdaQueryWrapper<RoomFriend>()
                        .eq(RoomFriend::getRoomKey, roomKey));
        if (rf == null) {
            throw new BusinessException(ResultCode.CONVERSATION_NOT_FOUND,
                    "还没有会话，请先通过『相亲桥』发起聊天申请");
        }
        // room_key 已隐含成员关系,防御性再校验
        if (!userId.equals(rf.getUid1()) && !userId.equals(rf.getUid2())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 确保双方都有 contact
        ensureContact(userId, rf.getRoomId());
        ensureContact(targetUserId, rf.getRoomId());

        // 构建返回
        return buildConvVO(rf.getRoomId(), userId, targetUserId);
    }

    /** 创建与指定用户的会话(仅供聊天申请批准流程调用;并发下按 uk_room_key 幂等) */
    @Override
    public ConversationVO createConversation(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能和自己聊天");
        }

        long uid1 = Math.min(userId, targetUserId);
        long uid2 = Math.max(userId, targetUserId);
        String roomKey = uid1 + "_" + uid2;

        // 并发兜底:已存在则直接复用
        RoomFriend rf = roomFriendMapper.selectOne(
                new LambdaQueryWrapper<RoomFriend>()
                        .eq(RoomFriend::getRoomKey, roomKey));
        if (rf != null) {
            ensureContact(userId, rf.getRoomId());
            ensureContact(targetUserId, rf.getRoomId());
            return buildConvVO(rf.getRoomId(), userId, targetUserId);
        }

        // 与被拉黑者不能建立新会话
        if (relationshipService.isBlockedEitherWay(userId, targetUserId)) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }

        // 创建 Room
        Room room = new Room();
        room.setType(1); // 单聊
        room.setActiveTime(LocalDateTime.now());
        roomMapper.insert(room);

        // 创建 RoomFriend(uk_room_key 唯一约束兜底并发:冲突后重新查询已存在房间)
        rf = new RoomFriend();
        rf.setRoomId(room.getId());
        rf.setUid1(uid1);
        rf.setUid2(uid2);
        rf.setRoomKey(roomKey);
        rf.setStatus(1); // normal
        try {
            roomFriendMapper.insert(rf);
        } catch (DuplicateKeyException e) {
            RoomFriend existing = roomFriendMapper.selectOne(
                    new LambdaQueryWrapper<RoomFriend>()
                            .eq(RoomFriend::getRoomKey, roomKey));
            if (existing == null) throw e;
            rf = existing;
        }

        // 确保双方都有 contact
        ensureContact(userId, rf.getRoomId());
        ensureContact(targetUserId, rf.getRoomId());

        // 构建返回
        return buildConvVO(rf.getRoomId(), userId, targetUserId);
    }

    /**
     * 房间成员鉴权:查询 room_friend 并确认当前用户为 uid1 或 uid2。
     * 房间不存在 → CONVERSATION_NOT_FOUND;存在但非成员 → FORBIDDEN。
     */
    private RoomFriend requireRoomMember(Long userId, Long roomId) {
        if (roomId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话不存在");
        }
        RoomFriend rf = roomFriendMapper.selectOne(
                new LambdaQueryWrapper<RoomFriend>()
                        .eq(RoomFriend::getRoomId, roomId));
        if (rf == null) {
            throw new BusinessException(ResultCode.CONVERSATION_NOT_FOUND);
        }
        if (!userId.equals(rf.getUid1()) && !userId.equals(rf.getUid2())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return rf;
    }

    @Override
    public List<ConversationVO> listConversations(Long userId) {
        // 从 contact 表查询会话列表(隐藏会话不出现在列表;置顶优先,再按活跃时间倒序)
        List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, userId)
                        .eq(Contact::getHidden, 0)
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
            userMapper.selectBatchIds(targetUids).forEach(u -> userMap.put(u.getId(), u));
        }

        // 批量:每房间最后一条可见消息 + 未读数(替换原 per-room N+1 循环)
        Map<Long, PrivateChat> lastMsgMap = new HashMap<>();
        for (PrivateChat m : privateChatMapper.selectLastVisibleMessageByRoom(userId, roomIds)) {
            lastMsgMap.put(m.getRoomId(), m);
        }
        Map<Long, Integer> unreadMap = new HashMap<>();
        for (Map<String, Object> row : privateChatMapper.countUnreadByRoom(userId, roomIds)) {
            Object rid = row.get("roomId");
            Object cnt = row.get("cnt");
            if (rid != null && cnt != null) {
                unreadMap.put(((Number) rid).longValue(), ((Number) cnt).intValue());
            }
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

            vo.setUnreadCount(unreadMap.getOrDefault(contact.getRoomId(), 0));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public ChatMessageVO sendMessage(Long userId, MessageSendDTO dto) {
        userWriteGuard.checkWritable(userId);
        // 反骚扰限流:同用户 1 分钟内私信上限
        if (!rateLimiter.tryAcquire("msg:" + userId, 30, 60_000)) {
            throw new BusinessException(ResultCode.TOO_FREQUENT);
        }
        // 房间成员鉴权:房间不存在/非成员一律拒绝,并确定接收者(客户端不能指定任意 toUserId)
        RoomFriend rf = requireRoomMember(userId, dto.getRoomId());
        Long toUserId = rf.getUid1().equals(userId) ? rf.getUid2() : rf.getUid1();

        // 拉黑拦截:任一方拉黑对方都禁止私聊(含 room_friend.status=0 的拉黑房间)
        if (relationshipService.isBlockedEitherWay(userId, toUserId)
                || (rf.getStatus() != null && rf.getStatus() == 0)) {
            throw new BusinessException(ResultCode.RELATION_BLOCKED);
        }

        // 消息类型边界校验:仅 text / image
        String messageType = dto.getMessageType() != null ? dto.getMessageType() : "text";
        if (!"text".equals(messageType) && !"image".equals(messageType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的消息类型");
        }

        // XSS 清洗 + 违禁词拦截(清洗后可能为空,再拦)
        dto.setContent(XssUtil.clean(dto.getContent()));
        sensitiveWordFilter.assertClean(dto.getContent());
        if (!StringUtils.hasText(dto.getContent())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息内容不能为空");
        }

        // 图片消息只允许本站上传源,拒绝外部跟踪图片
        if ("image".equals(messageType) && !isTrustedImageUrl(dto.getContent())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片消息只允许使用本站上传的图片");
        }

        // 幂等:同一 senderId + clientMessageId 已存在 → 直接返回已有消息(弱网重试不重复落库)
        if (StringUtils.hasText(dto.getClientMessageId())) {
            PrivateChat existed = privateChatMapper.selectOne(new LambdaQueryWrapper<PrivateChat>()
                    .eq(PrivateChat::getFromUserId, userId)
                    .eq(PrivateChat::getClientMessageId, dto.getClientMessageId()));
            if (existed != null) {
                return toChatMessageVO(existed);
            }
        }

        // 保存消息（使用 room_id）
        PrivateChat chat = new PrivateChat();
        chat.setConversationId(dto.getRoomId()); // 兼容旧字段
        chat.setRoomId(dto.getRoomId());
        chat.setFromUserId(userId);
        chat.setToUserId(toUserId);
        chat.setContent(dto.getContent());
        chat.setMessageType(messageType);
        chat.setIsRead(0);
        chat.setClientMessageId(dto.getClientMessageId()); // null 时不落库
        // 回复/引用:被回复消息必须存在于同一房间
        if (dto.getReplyToMessageId() != null) {
            PrivateChat parent = privateChatMapper.selectById(dto.getReplyToMessageId());
            if (parent == null || !dto.getRoomId().equals(parent.getRoomId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "回复的消息不存在");
            }
            chat.setParentMessageId(parent.getId());
        }
        try {
            privateChatMapper.insert(chat);
        } catch (DuplicateKeyException e) {
            // 并发幂等:uk_from_client 唯一索引兜底,重新查询已存在消息
            PrivateChat existed = privateChatMapper.selectOne(new LambdaQueryWrapper<PrivateChat>()
                    .eq(PrivateChat::getFromUserId, userId)
                    .eq(PrivateChat::getClientMessageId, dto.getClientMessageId()));
            if (existed != null) {
                return toChatMessageVO(existed);
            }
            throw e;
        }

        // 事务内写 Outbox(与消息同事务):发布事件不丢失,RabbitMQ 恢复后由定时任务补发
        ChatOutbox outbox = new ChatOutbox();
        outbox.setMessageId(chat.getId());
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        chatOutboxMapper.insert(outbox);

        return toChatMessageVO(chat);
    }

    @Override
    public PageVO<ChatMessageVO> getMessageHistory(Long userId, Long roomId, Long lastId, int size) {
        // 房间成员鉴权(不存在→会话不存在;非成员→无权限)
        RoomFriend rf = requireRoomMember(userId, roomId);
        // 游标分页:首次取最新 size 条;有 lastId 取 id < lastId 的更早消息。按 id DESC 多查一条判 hasMore。
        int limit = Math.max(1, Math.min(size, 100));
        LambdaQueryWrapper<PrivateChat> wrapper = new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getRoomId, roomId);
        if (rf.getUid1().equals(userId)) wrapper.eq(PrivateChat::getUid1Hidden, 0);
        else wrapper.eq(PrivateChat::getUid2Hidden, 0);
        if (lastId != null) wrapper.lt(PrivateChat::getId, lastId);
        wrapper.orderByDesc(PrivateChat::getId).last("LIMIT " + (limit + 1));

        List<PrivateChat> rows = privateChatMapper.selectList(wrapper);
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = new ArrayList<>(rows.subList(0, limit));
        // 反转为时间正序(旧的在前),前端按顺序展示
        Collections.reverse(rows);

        // 标记已读
        markConversationRead(userId, roomId);

        List<ChatMessageVO> records = rows.stream().map(this::toChatMessageVO).collect(Collectors.toList());
        PageVO<ChatMessageVO> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setHasMore(hasMore);
        return vo;
    }

    @Override
    public void markConversationRead(Long userId, Long roomId) {
        // 房间成员鉴权:非成员不可操作他人会话
        requireRoomMember(userId, roomId);
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
        // 房间成员鉴权:非成员不可读取他人会话设置
        requireRoomMember(userId, roomId);
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
        // 房间成员鉴权:非成员不可修改他人会话设置
        requireRoomMember(userId, roomId);
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
        // 房间成员鉴权:非成员不可搜索他人会话
        RoomFriend rf = requireRoomMember(userId, roomId);

        // 只搜索我这一侧未隐藏的消息(单侧清空后不再出现在搜索结果)
        LambdaQueryWrapper<PrivateChat> wrapper = new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getRoomId, roomId)
                .like(StringUtils.hasText(keyword), PrivateChat::getContent, keyword);
        if (rf.getUid1().equals(userId)) wrapper.eq(PrivateChat::getUid1Hidden, 0);
        else wrapper.eq(PrivateChat::getUid2Hidden, 0);
        Page<PrivateChat> page = new Page<>(1, Math.min(size, 100));
        Page<PrivateChat> result = privateChatMapper.selectPage(page, wrapper.orderByDesc(PrivateChat::getCreatedAt));

        List<ChatMessageVO> records = result.getRecords().stream()
                .map(this::toChatMessageVO)
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), 1, size);
    }

    @Override
    @Transactional
    public void clearMessages(Long userId, Long roomId) {
        // 房间成员鉴权(非成员→无权限),rf 供单侧清空判断
        RoomFriend rf = requireRoomMember(userId, roomId);
        if (findContact(userId, roomId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话不存在");
        }
        // 只把"我"这一侧的消息标记为已清空,对方视角不受影响
        PrivateChat update = new PrivateChat();
        if (rf.getUid1().equals(userId)) {
            update.setUid1Hidden(1);
        } else {
            update.setUid2Hidden(1);
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
        // 反骚扰限流:同一举报人 1 小时内举报上限
        if (!rateLimiter.tryAcquire("report:" + fromUserId, 10, 3_600_000)) {
            throw new BusinessException(ResultCode.TOO_FREQUENT);
        }
        // 基于房间的举报:举报者必须是房间成员,且被投诉人是房间另一成员
        if (roomId != null) {
            RoomFriend rf = requireRoomMember(fromUserId, roomId);
            Long other = rf.getUid1().equals(fromUserId) ? rf.getUid2() : rf.getUid1();
            if (!other.equals(toUserId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "不能投诉该会话外的用户");
            }
        }
        // 快照:记录被投诉人公开资料,避免无证据的举报记录
        User target = userMapper.selectById(toUserId);
        Report report = new Report();
        report.setFromUserId(fromUserId);
        report.setTargetUserId(toUserId);
        report.setRoomId(roomId);
        report.setTargetType("user");
        report.setReason(reason);
        report.setContentSnapshot(target != null ? "昵称:" + target.getNickname() : null);
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
        // 房间成员鉴权:非参与者不可撤回该房间消息
        requireRoomMember(userId, chat.getRoomId());
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

    @Override
    public void hideConversation(Long userId, Long roomId, boolean hidden) {
        requireRoomMember(userId, roomId);
        contactMapper.update(null, new LambdaUpdateWrapper<Contact>()
                .eq(Contact::getUid, userId)
                .eq(Contact::getRoomId, roomId)
                .set(Contact::getHidden, hidden ? 1 : 0));
    }

    private Contact findContact(Long uid, Long roomId) {
        return contactMapper.selectOne(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getUid, uid)
                .eq(Contact::getRoomId, roomId));
    }

    /** 消息实体 → VO(发送回执 / 历史 / 搜索共用) */
    private ChatMessageVO toChatMessageVO(PrivateChat m) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(m.getId());
        vo.setRoomId(m.getRoomId());
        vo.setFromUserId(m.getFromUserId());
        vo.setToUserId(m.getToUserId());
        vo.setContent(m.getContent());
        vo.setMessageType(m.getMessageType());
        vo.setIsRecalled(m.getIsRecalled());
        vo.setIsRead(m.getIsRead());
        vo.setParentMessageId(m.getParentMessageId());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }

    /** 图片消息只允许本站上传代理 URL(/api/v1/images/),拒绝外部跟踪图片 */
    private boolean isTrustedImageUrl(String url) {
        return url != null && url.startsWith("/api/v1/images/");
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

    /** 确保用户在 room 中有 contact 记录(uk_uid_room 唯一约束下幂等,并发重复插入忽略) */
    private void ensureContact(Long uid, Long roomId) {
        Contact contact = contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getUid, uid)
                        .eq(Contact::getRoomId, roomId));
        if (contact == null) {
            try {
                contact = new Contact();
                contact.setUid(uid);
                contact.setRoomId(roomId);
                contact.setActiveTime(LocalDateTime.now());
                contact.setReadTime(LocalDateTime.now());
                contactMapper.insert(contact);
            } catch (DuplicateKeyException e) {
                // 并发已插入(uk_uid_room),无需处理
            }
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
