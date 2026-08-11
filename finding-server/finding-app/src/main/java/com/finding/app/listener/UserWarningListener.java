package com.finding.app.listener;

import com.finding.message.service.MessageService;
import com.finding.user.event.UserWarningEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 警告事件 → 站内通知被警告用户 */
@Component
@RequiredArgsConstructor
public class UserWarningListener {

    private final MessageService messageService;

    @EventListener
    public void onWarning(UserWarningEvent e) {
        messageService.notify(e.getOperatorId(), e.getUserId(), "system_warning",
                "你收到一条平台警告：" + e.getReason(), e.getUserId());
    }
}
