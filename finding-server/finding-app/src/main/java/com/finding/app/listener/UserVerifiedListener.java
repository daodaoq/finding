package com.finding.app.listener;

import com.finding.message.service.MessageService;
import com.finding.user.event.UserVerifiedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 学生认证审核事件 → 站内通知被审核用户。
 * 通过:通知"已通过";驳回:通知原因并引导重新提交。
 */
@Component
@RequiredArgsConstructor
public class UserVerifiedListener {

    private final MessageService messageService;

    @EventListener
    public void onVerified(UserVerifiedEvent e) {
        if (e.isApproved()) {
            messageService.notify(e.getOperatorId(), e.getUserId(), "verify_approved",
                    "恭喜！你的学生实名认证已审核通过", e.getUserId());
        } else {
            String reason = (e.getComment() == null || e.getComment().isBlank())
                    ? "资料不符合要求" : e.getComment();
            messageService.notify(e.getOperatorId(), e.getUserId(), "verify_rejected",
                    "你的学生实名认证未通过：" + reason + "，请修改后重新提交", e.getUserId());
        }
    }
}
