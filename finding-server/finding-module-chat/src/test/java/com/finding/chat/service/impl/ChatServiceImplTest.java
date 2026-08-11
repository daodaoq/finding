package com.finding.chat.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.chat.dto.MessageSendDTO;
import com.finding.chat.entity.ChatOutbox;
import com.finding.chat.entity.Contact;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.Report;
import com.finding.chat.entity.Room;
import com.finding.chat.entity.RoomFriend;
import com.finding.chat.entity.StrangerMessage;
import com.finding.chat.mapper.ChatOutboxMapper;
import com.finding.chat.mapper.ContactMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.ReportMapper;
import com.finding.chat.mapper.RoomFriendMapper;
import com.finding.chat.mapper.RoomMapper;
import com.finding.chat.mapper.StrangerMessageMapper;
import com.finding.chat.vo.ChatMessageVO;
import com.finding.message.service.MessageService;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.framework.util.InMemoryRateLimiter;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.message.vo.ConversationVO;
import com.finding.user.common.VerificationGuard;
import com.finding.user.entity.User;
import com.finding.user.entity.UserSettings;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserWriteGuard;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 聊天服务安全回归单测 —— P0 安全三连:
 * 1) 房间成员鉴权(IDOR 越权读历史/已读/搜索/清空/设置/撤回);
 * 2) 会话创建接入聊天申请(getConversation 只查不建 / createConversation 仅供批准流程);
 * 3) sendMessage 以 roomId 定位会话并从房间推导接收者。
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock private PrivateChatMapper privateChatMapper;
    @Mock private RoomMapper roomMapper;
    @Mock private RoomFriendMapper roomFriendMapper;
    @Mock private ContactMapper contactMapper;
    @Mock private UserMapper userMapper;
    @Mock private ReportMapper reportMapper;
    @Mock private UserSettingsMapper userSettingsMapper;
    @Mock private ChatOutboxMapper chatOutboxMapper;
    @Mock private StrangerMessageMapper strangerMessageMapper;
    @Mock private MessageService messageService;
    @Mock private VerificationGuard verificationGuard;
    @Mock private SensitiveWordFilter sensitiveWordFilter;
    @Mock private UserRelationshipService relationshipService;
    @Mock private UserWriteGuard userWriteGuard;
    @Mock private InMemoryRateLimiter rateLimiter;
    @Mock private WebSocketServer webSocketServer;

    @InjectMocks
    private ChatServiceImpl service;

    /** 放行反骚扰限流(mock 默认 false 会让所有发送被限流) */
    private void allowRateLimit() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
    }

    @BeforeEach
    void initMybatisLambdaCache() {
        // 纯单测无 Spring 上下文:注册实体 TableInfo,使 LambdaQuery/LambdaUpdateWrapper 可解析列名
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RoomFriend.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Contact.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PrivateChat.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Room.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Report.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserSettings.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ChatOutbox.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StrangerMessage.class);
    }

    private RoomFriend rf(Long uid1, Long uid2, Long roomId) {
        RoomFriend rf = new RoomFriend();
        rf.setUid1(uid1);
        rf.setUid2(uid2);
        rf.setRoomId(roomId);
        rf.setStatus(1);
        return rf;
    }

    private MessageSendDTO dto(Long roomId, String content) {
        MessageSendDTO d = new MessageSendDTO();
        d.setRoomId(roomId);
        d.setContent(content);
        return d;
    }

    private PrivateChat msg(Long id, Long roomId, Long from, Long to) {
        PrivateChat m = new PrivateChat();
        m.setId(id);
        m.setRoomId(roomId);
        m.setFromUserId(from);
        m.setToUserId(to);
        m.setContent("hi");
        m.setMessageType("text");
        m.setIsRecalled(0);
        m.setIsRead(0);
        return m;
    }

    // ── P0-1 房间成员鉴权 ──

    @Test
    void getMessageHistory_nonMember_throwsForbidden() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getMessageHistory(3L, 100L, null, 50));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void getMessageHistory_noRoom_throwsConversationNotFound() {
        when(roomFriendMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getMessageHistory(3L, 999L, null, 50));
        assertEquals(ResultCode.CONVERSATION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getMessageHistory_member_returnsMessages() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(privateChatMapper.selectList(any())).thenReturn(List.of(msg(10L, 100L, 2L, 1L)));

        PageVO<ChatMessageVO> result = service.getMessageHistory(1L, 100L, null, 50);
        assertEquals(1, result.getRecords().size());
        assertEquals(10L, result.getRecords().get(0).getId());
        assertFalse(result.getHasMore());
    }

    @Test
    void getMessageHistory_member_cursorHasMore_andReversesToAsc() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        // 多查一条:limit=2 → 返回 3 条 → hasMore=true,仅保留 2 条
        when(privateChatMapper.selectList(any())).thenReturn(List.of(
                msg(30L, 100L, 2L, 1L), msg(20L, 100L, 2L, 1L), msg(10L, 100L, 2L, 1L)));

        PageVO<ChatMessageVO> result = service.getMessageHistory(1L, 100L, null, 2);
        assertTrue(result.getHasMore());
        assertEquals(2, result.getRecords().size());
        // 保留最新的 2 条(id DESC: 30,20)反转为时间正序展示: 20,30
        assertEquals(20L, result.getRecords().get(0).getId());
        assertEquals(30L, result.getRecords().get(1).getId());
    }

    @Test
    void markConversationRead_nonMember_throwsForbidden() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.markConversationRead(3L, 100L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void searchMessages_nonMember_throwsForbidden() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.searchMessages(3L, 100L, "hi", 50));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void clearMessages_nonMember_throwsForbidden() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.clearMessages(3L, 100L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void updateConversationSettings_nonMember_throwsForbidden() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateConversationSettings(3L, 100L, true, null, null));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void recallMessage_nonParticipant_throwsForbidden() {
        when(privateChatMapper.selectById(50L)).thenReturn(msg(50L, 100L, 1L, 2L));
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.recallMessage(3L, 50L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // ── P0-3 会话创建接入聊天申请 ──

    @Test
    void getConversation_noRoom_throwsConversationNotFound() {
        when(roomFriendMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getConversation(1L, 2L));
        assertEquals(ResultCode.CONVERSATION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getConversation_existingRoom_returnsVO() {
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(contactMapper.selectOne(any())).thenReturn(null); // ensureContact 需插入
        when(contactMapper.insert(any())).thenReturn(1);
        when(privateChatMapper.selectList(any())).thenReturn(List.of());
        User target = new User();
        target.setNickname("阿宝");
        when(userMapper.selectById(2L)).thenReturn(target);

        ConversationVO vo = service.getConversation(1L, 2L);
        assertEquals(100L, vo.getRoomId());
        assertEquals(2L, vo.getTargetUserId());
        assertEquals("阿宝", vo.getTargetNickname());
    }

    @Test
    void createConversation_createsRoomAndContact() {
        when(roomFriendMapper.selectOne(any())).thenReturn(null);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(roomMapper.insert(any())).thenReturn(1);
        when(roomFriendMapper.insert(any())).thenReturn(1);
        when(contactMapper.selectOne(any())).thenReturn(null);
        when(contactMapper.insert(any())).thenReturn(1);
        when(privateChatMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectById(2L)).thenReturn(new User());

        ConversationVO vo = service.createConversation(1L, 2L);

        ArgumentCaptor<RoomFriend> rfCaptor = ArgumentCaptor.forClass(RoomFriend.class);
        verify(roomFriendMapper).insert(rfCaptor.capture());
        RoomFriend inserted = rfCaptor.getValue();
        assertEquals(1L, inserted.getUid1());
        assertEquals(2L, inserted.getUid2());
        assertEquals("1_2", inserted.getRoomKey());
        assertEquals(1, inserted.getStatus());
        assertEquals(2L, vo.getTargetUserId());
    }

    @Test
    void createConversation_duplicateRoomKey_returnsExisting() {
        // 第一次 selectOne(初查不存在) → null;冲突后重新查询 → 已存在房间 200
        when(roomFriendMapper.selectOne(any())).thenReturn(null, rf(1L, 2L, 200L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(roomMapper.insert(any())).thenReturn(1);
        when(roomFriendMapper.insert(any())).thenThrow(new DuplicateKeyException("dup"));
        when(contactMapper.selectOne(any())).thenReturn(null);
        when(contactMapper.insert(any())).thenReturn(1);
        when(privateChatMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectById(2L)).thenReturn(new User());

        ConversationVO vo = service.createConversation(1L, 2L);
        assertEquals(200L, vo.getRoomId());
    }

    @Test
    void createConversation_blocked_throwsRelationBlocked() {
        when(roomFriendMapper.selectOne(any())).thenReturn(null);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createConversation(1L, 2L));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    // ── P0-3 sendMessage 以 roomId 定位会话并推导接收者 ──

    @Test
    void sendMessage_noRoom_throwsConversationNotFound() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(1L, dto(999L, "hi")));
        assertEquals(ResultCode.CONVERSATION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void sendMessage_nonMember_throwsForbidden() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(3L, dto(100L, "hi")));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void sendMessage_member_derivesToUserIdFromRoom_andWritesOutbox() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(privateChatMapper.insert(any())).thenAnswer(inv -> {
            PrivateChat c = inv.getArgument(0);
            c.setId(555L);
            return 1;
        });
        when(chatOutboxMapper.insert(any())).thenReturn(1);

        MessageSendDTO dto = dto(100L, "hi");
        dto.setClientMessageId("client-1");
        ChatMessageVO result = service.sendMessage(1L, dto);

        ArgumentCaptor<PrivateChat> captor = ArgumentCaptor.forClass(PrivateChat.class);
        verify(privateChatMapper).insert(captor.capture());
        PrivateChat inserted = captor.getValue();
        assertEquals(1L, inserted.getFromUserId());
        assertEquals(2L, inserted.getToUserId());
        assertEquals(100L, inserted.getRoomId());
        assertEquals(100L, inserted.getConversationId());
        assertEquals("client-1", inserted.getClientMessageId());
        // 返回真实消息回执
        assertNotNull(result.getId());
        assertEquals(555L, result.getId());
        assertEquals("hi", result.getContent());
        // 事务内写 outbox(与消息同事务,发布事件不丢失)
        ArgumentCaptor<ChatOutbox> outboxCaptor = ArgumentCaptor.forClass(ChatOutbox.class);
        verify(chatOutboxMapper).insert(outboxCaptor.capture());
        assertEquals(555L, outboxCaptor.getValue().getMessageId());
        assertEquals(0, outboxCaptor.getValue().getStatus());
    }

    @Test
    void sendMessage_blocked_throwsRelationBlocked() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(1L, dto(100L, "hi")));
        assertEquals(ResultCode.RELATION_BLOCKED.getCode(), ex.getCode());
    }

    // ── P1-2 clientMessageId 幂等 / P1-7 边界校验 ──

    @Test
    void sendMessage_idempotentClientMessageId_returnsExistingWithoutInsert() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(privateChatMapper.selectOne(any())).thenReturn(msg(99L, 100L, 1L, 2L));

        MessageSendDTO dto = dto(100L, "hi");
        dto.setClientMessageId("client-1");
        ChatMessageVO result = service.sendMessage(1L, dto);

        assertEquals(99L, result.getId());
        verify(privateChatMapper, never()).insert(any());
        verify(chatOutboxMapper, never()).insert(any());
    }

    @Test
    void sendMessage_invalidMessageType_rejected() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);

        MessageSendDTO dto = dto(100L, "x");
        dto.setMessageType("video");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.sendMessage(1L, dto));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void sendMessage_untrustedImageUrl_rejected() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);

        MessageSendDTO dto = dto(100L, "https://evil.com/track.png");
        dto.setMessageType("image");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.sendMessage(1L, dto));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("本站上传"));
    }

    @Test
    void sendMessage_trustedImageUrl_ok() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(privateChatMapper.insert(any())).thenReturn(1);
        when(chatOutboxMapper.insert(any())).thenReturn(1);

        MessageSendDTO dto = dto(100L, "/api/v1/images/abc.png");
        dto.setMessageType("image");
        assertDoesNotThrow(() -> service.sendMessage(1L, dto));

        ArgumentCaptor<PrivateChat> captor = ArgumentCaptor.forClass(PrivateChat.class);
        verify(privateChatMapper).insert(captor.capture());
        assertEquals("image", captor.getValue().getMessageType());
    }

    // ── P2-5a 回复/引用 + P2-5b 反骚扰限流 ──

    @Test
    void sendMessage_replyToMessage_invalidRoom_rejected() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        // 被回复消息在另一个房间
        when(privateChatMapper.selectById(999L)).thenReturn(msg(999L, 200L, 2L, 1L));

        MessageSendDTO dto = dto(100L, "hi");
        dto.setReplyToMessageId(999L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.sendMessage(1L, dto));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void sendMessage_replyToMessage_sameRoom_storesParent() {
        allowRateLimit();
        when(roomFriendMapper.selectOne(any())).thenReturn(rf(1L, 2L, 100L));
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(privateChatMapper.selectById(999L)).thenReturn(msg(999L, 100L, 2L, 1L)); // 同房间
        when(privateChatMapper.insert(any())).thenReturn(1);
        when(chatOutboxMapper.insert(any())).thenReturn(1);

        MessageSendDTO dto = dto(100L, "hi");
        dto.setReplyToMessageId(999L);
        service.sendMessage(1L, dto);

        ArgumentCaptor<PrivateChat> captor = ArgumentCaptor.forClass(PrivateChat.class);
        verify(privateChatMapper).insert(captor.capture());
        assertEquals(999L, captor.getValue().getParentMessageId());
    }

    @Test
    void sendMessage_rateLimited_throwsTooFrequent() {
        // 不调用 allowRateLimit():mock 默认 false → 触发反骚扰限流
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(1L, dto(100L, "hi")));
        assertEquals(ResultCode.TOO_FREQUENT.getCode(), ex.getCode());
    }

    // ── 陌生人打招呼消息 ──

    @Test
    void sendStrangerMessage_noConversation_insertsAndNotifies() {
        User target = new User(); target.setId(2L); target.setStatus(1);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(roomFriendMapper.selectCount(any())).thenReturn(0L); // 无正式会话
        when(strangerMessageMapper.selectCount(any())).thenReturn(0L); // 未发过
        when(strangerMessageMapper.insert(any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(new User());

        service.sendStrangerMessage(1L, 2L, "你好");

        verify(strangerMessageMapper).insert(any());
        verify(messageService).notify(any(), any(), any(), any(), any());
    }

    @Test
    void sendStrangerMessage_alreadySent_rejected() {
        User target = new User(); target.setId(2L); target.setStatus(1);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(relationshipService.isBlockedEitherWay(1L, 2L)).thenReturn(false);
        when(roomFriendMapper.selectCount(any())).thenReturn(0L);
        when(strangerMessageMapper.selectCount(any())).thenReturn(1L); // 已发过

        BusinessException ex = assertThrows(BusinessException.class, () -> service.sendStrangerMessage(1L, 2L, "你好"));
        assertEquals(ResultCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(strangerMessageMapper, never()).insert(any());
    }

    @Test
    void acceptStrangerMessage_alreadyHandled_rejected() {
        StrangerMessage msg = new StrangerMessage();
        msg.setId(10L); msg.setFromUserId(2L); msg.setToUserId(1L); msg.setStatus(0);
        when(strangerMessageMapper.selectById(10L)).thenReturn(msg);
        when(strangerMessageMapper.update(any(), any())).thenReturn(0); // 已被并发处理

        BusinessException ex = assertThrows(BusinessException.class, () -> service.acceptStrangerMessage(1L, 10L));
        assertEquals(ResultCode.CHAT_APPLY_ALREADY_HANDLED.getCode(), ex.getCode());
    }
}
