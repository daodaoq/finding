package com.finding.bridge.event;

import com.finding.bridge.mapper.UserLikeMapper;
import com.finding.bridge.mapper.UserMatchMapper;
import com.finding.common.event.UserBlockedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchBlockListenerTest {

    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private UserMatchMapper userMatchMapper;

    private MatchBlockListener listener;

    @BeforeEach
    void setUp() {
        listener = new MatchBlockListener(userLikeMapper, userMatchMapper);
    }

    @Test
    void onUserBlocked_deletesLikesAndMatch() {
        listener.onUserBlocked(new UserBlockedEvent(1L, 2L));

        verify(userLikeMapper).delete(any());
        verify(userMatchMapper).delete(any());
    }
}
