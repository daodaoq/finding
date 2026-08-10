package com.finding.chat.event;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finding.chat.constant.ChatApplyStatus;
import com.finding.chat.entity.ChatApply;
import com.finding.chat.mapper.ChatApplyMapper;
import com.finding.common.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 拉黑联动:任一方拉黑后,自动将双方之间待处理的聊天申请置为「已撤回」,
 * 接收人不可再处理。
 */
@Component
@RequiredArgsConstructor
public class ChatApplyBlockListener {

    private final ChatApplyMapper chatApplyMapper;

    @EventListener
    public void onUserBlocked(UserBlockedEvent event) {
        chatApplyMapper.update(null, new LambdaUpdateWrapper<ChatApply>()
                .eq(ChatApply::getStatus, ChatApplyStatus.PENDING.getCode())
                .and(w -> w.and(x -> x.eq(ChatApply::getFromUserId, event.getUserId())
                                        .eq(ChatApply::getToUserId, event.getBlockedUserId()))
                        .or().and(x -> x.eq(ChatApply::getFromUserId, event.getBlockedUserId())
                                        .eq(ChatApply::getToUserId, event.getUserId())))
                .set(ChatApply::getStatus, ChatApplyStatus.CANCELLED.getCode())
                .set(ChatApply::getHandleTime, LocalDateTime.now()));
    }
}
