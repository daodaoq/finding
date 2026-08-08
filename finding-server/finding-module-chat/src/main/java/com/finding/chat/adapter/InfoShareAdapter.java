package com.finding.chat.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.chat.entity.InfoShare;
import com.finding.chat.mapper.InfoShareMapper;
import com.finding.user.service.InfoShareQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 信息互换状态查询端口实现 —— 由 chat 模块提供,注入给 user 模块使用。
 * 使 user 模块的「情感简历」查看能校验是否已互换信息,且不产生编译期环依赖。
 */
@Component
@RequiredArgsConstructor
public class InfoShareAdapter implements InfoShareQuery {

    private final InfoShareMapper infoShareMapper;

    @Override
    public int getShareStatus(Long uidA, Long uidB) {
        InfoShare share = infoShareMapper.selectOne(new LambdaQueryWrapper<InfoShare>()
                .and(w -> w
                        .eq(InfoShare::getFromUserId, uidA).eq(InfoShare::getToUserId, uidB)
                        .or()
                        .eq(InfoShare::getFromUserId, uidB).eq(InfoShare::getToUserId, uidA))
                .orderByDesc(InfoShare::getCreatedAt)
                .last("LIMIT 1"));
        if (share == null) return STATUS_NONE;
        return switch (share.getStatus()) {
            case 0 -> STATUS_PENDING;
            case 1 -> STATUS_APPROVED;
            case 2 -> STATUS_REJECTED;
            default -> STATUS_NONE;
        };
    }
}
