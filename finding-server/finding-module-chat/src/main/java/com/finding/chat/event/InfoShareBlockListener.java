package com.finding.chat.event;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finding.chat.entity.InfoShare;
import com.finding.chat.mapper.InfoShareMapper;
import com.finding.common.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 拉黑联动:任一方拉黑后,自动将双方之间待处理的信息互换申请置为「已拒绝」,
 * 接收人不可再处理(状态 2 语义上同时代表"已失效",后续再申请走拒绝后重试流程)。
 */
@Component
@RequiredArgsConstructor
public class InfoShareBlockListener {

    private final InfoShareMapper infoShareMapper;

    @EventListener
    public void onUserBlocked(UserBlockedEvent event) {
        infoShareMapper.update(null, new LambdaUpdateWrapper<InfoShare>()
                .eq(InfoShare::getStatus, 0)
                .and(w -> w.and(x -> x.eq(InfoShare::getFromUserId, event.getUserId())
                                        .eq(InfoShare::getToUserId, event.getBlockedUserId()))
                        .or().and(x -> x.eq(InfoShare::getFromUserId, event.getBlockedUserId())
                                        .eq(InfoShare::getToUserId, event.getUserId())))
                .set(InfoShare::getStatus, 2)
                .set(InfoShare::getHandledAt, LocalDateTime.now()));
    }
}
