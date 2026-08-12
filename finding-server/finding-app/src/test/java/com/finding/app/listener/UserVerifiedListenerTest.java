package com.finding.app.listener;

import com.finding.message.service.MessageService;
import com.finding.user.event.UserVerifiedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * 认证审核事件 → 站内通知:通过/驳回(带原因/默认原因)三类通知内容。
 */
@ExtendWith(MockitoExtension.class)
class UserVerifiedListenerTest {

    @Mock
    private MessageService messageService;

    private UserVerifiedListener listener;

    @BeforeEach
    void setUp() {
        listener = new UserVerifiedListener(messageService);
    }

    @Test
    void approved_notifiesVerifyApproved() {
        listener.onVerified(new UserVerifiedEvent(100L, true, null, 9L));

        verify(messageService).notify(9L, 100L, "verify_approved",
                "恭喜！你的学生实名认证已审核通过", 100L);
    }

    @Test
    void rejected_withComment_notifiesWithReason() {
        listener.onVerified(new UserVerifiedEvent(100L, false, "证件不清晰", 9L));

        verify(messageService).notify(9L, 100L, "verify_rejected",
                "你的学生实名认证未通过：证件不清晰，请修改后重新提交", 100L);
    }

    @Test
    void rejected_blankComment_usesDefaultReason() {
        listener.onVerified(new UserVerifiedEvent(100L, false, "  ", 9L));

        verify(messageService).notify(9L, 100L, "verify_rejected",
                "你的学生实名认证未通过：资料不符合要求，请修改后重新提交", 100L);
    }
}
