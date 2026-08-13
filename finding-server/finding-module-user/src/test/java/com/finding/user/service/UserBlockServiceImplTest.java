package com.finding.user.service;

import com.finding.common.BusinessException;
import com.finding.common.event.UserBlockedEvent;
import com.finding.user.entity.User;
import com.finding.user.entity.UserBlock;
import com.finding.user.mapper.UserBlockMapper;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.impl.UserBlockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceImplTest {

    @Mock
    private UserBlockMapper userBlockMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserFollowMapper userFollowMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserBlockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserBlockServiceImpl(userBlockMapper, userMapper, userFollowMapper, eventPublisher);
    }

    @Test
    void blockSelf_throws() {
        assertThrows(BusinessException.class, () -> service.block(1L, 1L));
        verify(userBlockMapper, never()).insert(any());
    }

    @Test
    void block_insertsAndCleansFollowAndPublishesEvent() {
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(userBlockMapper.selectCount(any())).thenReturn(0L);

        service.block(1L, 2L);

        verify(userBlockMapper).insert(any(UserBlock.class));
        // 双向关注被清理
        verify(userFollowMapper).delete(any());
        // 联动事件发布(供 bridge 清理心动/配对)
        ArgumentCaptor<UserBlockedEvent> cap = ArgumentCaptor.forClass(UserBlockedEvent.class);
        verify(eventPublisher).publishEvent(cap.capture());
        assertEquals(1L, cap.getValue().getUserId());
        assertEquals(2L, cap.getValue().getBlockedUserId());
    }

    @Test
    void alreadyBlocked_throws() {
        when(userMapper.selectById(2L)).thenReturn(new User());
        when(userBlockMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.block(1L, 2L));
        verify(userBlockMapper, never()).insert(any());
        verify(userFollowMapper, never()).delete(any());
    }
}
