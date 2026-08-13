package com.finding.framework.websocket;

import com.finding.common.RedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 在线状态服务单测 —— Redis 心跳键标记/删除/查询。 */
@ExtendWith(MockitoExtension.class)
class OnlineStatusServiceTest {

    @Mock private RedisUtils redisUtils;
    @InjectMocks private OnlineStatusService service;

    @Test
    void markOnline_setsWithTtl() {
        service.markOnline(1L);
        verify(redisUtils).set("presence:online:1", "1", 75, TimeUnit.SECONDS);
    }

    @Test
    void markOffline_deletes() {
        service.markOffline(1L);
        verify(redisUtils).delete("presence:online:1");
    }

    @Test
    void isOnline_true() {
        when(redisUtils.exists("presence:online:1")).thenReturn(true);
        assertTrue(service.isOnline(1L));
    }

    @Test
    void isOnline_false() {
        when(redisUtils.exists("presence:online:9")).thenReturn(false);
        assertFalse(service.isOnline(9L));
    }

    @Test
    void isOnline_null_returnsFalse() {
        assertFalse(service.isOnline(null));
    }

    @Test
    void isOnlineBatch_mapsAll() {
        when(redisUtils.exists(any())).thenReturn(true);
        Map<Long, Boolean> m = service.isOnlineBatch(List.of(1L, 2L));
        assertEquals(2, m.size());
        assertTrue(m.get(1L));
        assertTrue(m.get(2L));
    }
}
