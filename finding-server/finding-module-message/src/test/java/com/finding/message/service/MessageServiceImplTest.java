package com.finding.message.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.message.entity.Message;
import com.finding.message.event.NewNotificationEvent;
import com.finding.message.mapper.ConversationMapper;
import com.finding.message.mapper.MessageMapper;
import com.finding.message.service.impl.MessageServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 站内通知服务单测 —— 写入/未读计数/已读归属校验。 */
class MessageServiceImplTest {

    @Mock private MessageMapper messageMapper;
    @Mock private ConversationMapper conversationMapper;
    @Mock private com.finding.user.mapper.UserMapper userMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MessageServiceImpl(messageMapper, conversationMapper, userMapper, eventPublisher);
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Message.class);
    }

    @Test
    void notify_insertsAndPublishesEvent() {
        when(messageMapper.insert(any())).thenReturn(1);

        service.notify(1L, 2L, "like", "赞了你的动态", 100L);

        ArgumentCaptor<Message> cap = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(cap.capture());
        assertEquals("like", cap.getValue().getType());
        assertEquals(2L, cap.getValue().getToUserId());
        verify(eventPublisher).publishEvent(any(NewNotificationEvent.class));
    }

    @Test
    void getUnreadCount_countsOnlyUnread() {
        when(messageMapper.selectCount(any())).thenReturn(3L);

        assertEquals(3, service.getUnreadCount(1L));
    }

    @Test
    void markAsRead_othersMessage_rejected() {
        Message msg = new Message();
        msg.setId(1L);
        msg.setToUserId(2L);
        when(messageMapper.selectById(1L)).thenReturn(msg);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.markAsRead(1L, 1L));
        assertEquals(ResultCode.MESSAGE_NOT_FOUND.getCode(), ex.getCode());
    }
}
