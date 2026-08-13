package com.finding.bridge.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.bridge.entity.UserLike;
import com.finding.bridge.entity.UserMatch;
import com.finding.bridge.mapper.UserLikeMapper;
import com.finding.bridge.mapper.UserMatchMapper;
import com.finding.common.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 拉黑联动:任一方拉黑后,清理双方之间残留的心动(user_like)与配对(user_match),
 * 避免「已拉黑却仍互相心动/配对」的不一致状态。
 */
@Component
@RequiredArgsConstructor
public class MatchBlockListener {

    private final UserLikeMapper userLikeMapper;
    private final UserMatchMapper userMatchMapper;

    @EventListener
    public void onUserBlocked(UserBlockedEvent event) {
        long uid1 = event.getUserId();
        long uid2 = event.getBlockedUserId();

        // 双向心动
        userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                .and(w -> w.and(x -> x.eq(UserLike::getLikerId, uid1).eq(UserLike::getLikedId, uid2))
                        .or().and(x -> x.eq(UserLike::getLikerId, uid2).eq(UserLike::getLikedId, uid1))));
        // 配对(规范化 a<b)
        long lo = Math.min(uid1, uid2);
        long hi = Math.max(uid1, uid2);
        userMatchMapper.delete(new LambdaQueryWrapper<UserMatch>()
                .eq(UserMatch::getUserAId, lo)
                .eq(UserMatch::getUserBId, hi));
    }
}
