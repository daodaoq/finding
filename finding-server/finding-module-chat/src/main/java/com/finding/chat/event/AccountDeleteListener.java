package com.finding.chat.event;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finding.chat.constant.ChatApplyStatus;
import com.finding.chat.entity.ChatApply;
import com.finding.chat.entity.InfoShare;
import com.finding.chat.mapper.ChatApplyMapper;
import com.finding.chat.mapper.InfoShareMapper;
import com.finding.common.event.AccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 账号注销联动:取消涉及被注销用户的待处理聊天申请与信息互换申请。
 * 访问令牌由 JWT 过滤器按 status!=1 即时失效;这里清理未决的关系记录。
 */
@Component
@RequiredArgsConstructor
public class AccountDeleteListener {

    private final ChatApplyMapper chatApplyMapper;
    private final InfoShareMapper infoShareMapper;

    @EventListener
    public void onAccountDeleted(AccountDeletedEvent event) {
        Long userId = event.getUserId();
        // 取消涉及该用户的待处理聊天申请(置为已撤回)
        chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode())
                .and(w -> w.eq(ChatApply::getFromUserId, userId)
                        .or().eq(ChatApply::getToUserId, userId))
                .set(ChatApply::getStatus, ChatApplyStatus.CANCELLED.getCode())
                .set(ChatApply::getHandleTime, LocalDateTime.now()));
        // 取消涉及该用户的待处理信息互换(置为已拒绝)
        infoShareMapper.update(null, new LambdaUpdateWrapper<InfoShare>()
                .eq(InfoShare::getStatus, 0)
                .and(w -> w.eq(InfoShare::getFromUserId, userId)
                        .or().eq(InfoShare::getToUserId, userId))
                .set(InfoShare::getStatus, 2)
                .set(InfoShare::getHandledAt, LocalDateTime.now()));
    }
}
