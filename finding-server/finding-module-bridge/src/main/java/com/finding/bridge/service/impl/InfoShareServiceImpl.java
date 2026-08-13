package com.finding.bridge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finding.bridge.constant.ChatApplyStatus;
import com.finding.bridge.constant.InfoShareStatus;
import com.finding.bridge.entity.ChatApply;
import com.finding.bridge.entity.InfoShare;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.RoomFriend;
import com.finding.bridge.event.InfoSharePushEvent;
import com.finding.bridge.mapper.ChatApplyMapper;
import com.finding.bridge.mapper.InfoShareMapper;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.RoomFriendMapper;
import com.finding.bridge.service.InfoShareService;
import com.finding.bridge.vo.InfoShareStatusVO;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.framework.util.RedisRateLimiter;
import com.finding.message.service.MessageService;
import com.finding.user.common.VerificationGuard;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserRelationshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfoShareServiceImpl implements InfoShareService {

    private final InfoShareMapper infoShareMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final VerificationGuard verificationGuard;
    private final UserRelationshipService relationshipService;
    private final RedisRateLimiter rateLimiter;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatApplyMapper chatApplyMapper;
    private final RoomFriendMapper roomFriendMapper;
    private final PrivateChatMapper privateChatMapper;

    @Override
    @Transactional
    public Long requestShare(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能给自己发送互换申请");
        }
        verificationGuard.checkVerified(fromUserId);
        // 反骚扰限流:同用户 1 小时最多 10 次互换申请
        if (!rateLimiter.tryAcquire("infoShare:" + fromUserId, 10, 3_600_000)) {
            throw new BusinessException(ResultCode.TOO_FREQUENT);
        }

        User targetUser = userMapper.selectById(toUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 统一发现权限:目标账号状态/可搜索/双向拉黑(与聊天申请同一入口)
        if (!relationshipService.canDiscover(fromUserId, toUserId)) {
            if (relationshipService.isBlockedEitherWay(fromUserId, toUserId)) {
                throw new BusinessException(ResultCode.RELATION_BLOCKED);
            }
            throw new BusinessException(ResultCode.USER_NOT_DISCOVERABLE);
        }
        // 业务规则:必须先建立聊天关系才能互换资料。
        // 聊天关系可经由三条路径建立:① 鹊桥心动申请任一方向通过;② 已建立单聊会话(room_friend);③ 有实际私信往来(兜底)。
        // ①②任一条缺失时③能覆盖"聊过天但无 room_friend/chat_apply"的历史/种子数据。
        long approvedApply = chatApplyMapper.selectCount(new LambdaQueryWrapper<ChatApply>()
                .eq(ChatApply::getStatus, ChatApplyStatus.APPROVED.getCode())
                .and(w -> w.and(x -> x.eq(ChatApply::getFromUserId, fromUserId).eq(ChatApply::getToUserId, toUserId))
                        .or().and(x -> x.eq(ChatApply::getFromUserId, toUserId).eq(ChatApply::getToUserId, fromUserId))));
        long roomFriends = countRoomFriend(fromUserId, toUserId);
        long privateMsgs = countPrivateChat(fromUserId, toUserId);
        boolean hasChatRelation = approvedApply > 0 || roomFriends > 0 || privateMsgs > 0;
        if (!hasChatRelation) {
            // 诊断日志:云端若仍被拒,从日志可看出三项计数哪个为 0,定位数据/部署问题
            log.warn("互换信息被拒(未建立聊天关系): from={}, to={}, approvedApply={}, roomFriend={}, privateMsgs={}",
                    fromUserId, toUserId, approvedApply, roomFriends, privateMsgs);
            throw new BusinessException(ResultCode.INFO_SHARE_NEED_CHAT);
        }

        // 任一方向已存在 pending/approved → 不允许重复发起(含反向,避免双方各持一条 pending)
        Long activeCount = infoShareMapper.selectCount(new LambdaQueryWrapper<InfoShare>()
                .and(w -> w
                        .eq(InfoShare::getFromUserId, fromUserId).eq(InfoShare::getToUserId, toUserId)
                        .or()
                        .eq(InfoShare::getFromUserId, toUserId).eq(InfoShare::getToUserId, fromUserId))
                .ne(InfoShare::getStatus, InfoShareStatus.REJECTED.getCode()));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_SENT, "已经发起过互换申请了");
        }

        // 同方向已被拒 → 原地改回 pending(允许重新申请,迁移 REJECTED->PENDING)
        InfoShare rejected = infoShareMapper.selectOne(new LambdaQueryWrapper<InfoShare>()
                .eq(InfoShare::getFromUserId, fromUserId)
                .eq(InfoShare::getToUserId, toUserId)
                .eq(InfoShare::getStatus, InfoShareStatus.REJECTED.getCode())
                .orderByDesc(InfoShare::getCreatedAt)
                .last("LIMIT 1"));

        Long shareId;
        if (rejected != null) {
            rejected.setStatus(InfoShareStatus.PENDING.getCode());
            rejected.setHandledAt(null);
            infoShareMapper.updateById(rejected);
            shareId = rejected.getId();
        } else {
            InfoShare share = new InfoShare();
            share.setFromUserId(fromUserId);
            share.setToUserId(toUserId);
            share.setStatus(InfoShareStatus.PENDING.getCode());
            try {
                infoShareMapper.insert(share);
            } catch (DuplicateKeyException e) {
                // 并发同方向重复发起:唯一约束 uk_from_to 兜底
                throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_SENT, "已经发起过互换申请了");
            }
            shareId = share.getId();
        }

        // 站内通知 + WebSocket 实时推送给接收方(WS 由事务提交后监听投递)
        User fromUser = userMapper.selectById(fromUserId);
        String nickname = fromUser != null ? fromUser.getNickname() : "有人";
        messageService.notify(fromUserId, toUserId, "info_share_request",
                nickname + " 想和你互换详细信息", shareId);
        // WS content 只放昵称,由前端弹窗拼完整文案
        eventPublisher.publishEvent(new InfoSharePushEvent(toUserId, "request", fromUserId, nickname, shareId));

        log.info("Info share request: user {} → user {}, shareId={}", fromUserId, toUserId, shareId);
        return shareId;
    }

    @Override
    @Transactional
    public void handleShare(Long userId, Long shareId, Integer status) {
        InfoShare share = infoShareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException(ResultCode.CHAT_APPLY_NOT_FOUND);
        }
        if (!share.getToUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无权处理该申请");
        }
        if (share.getStatus() != InfoShareStatus.PENDING.getCode()) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_HANDLED);
        }
        // 状态迁移校验:仅允许 PENDING -> APPROVED | REJECTED,非法流转直接拒绝
        InfoShareStatus to = InfoShareStatus.of(status);
        if (to == null || !InfoShareStatus.PENDING.canTransitTo(to)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法的状态流转");
        }
        // 条件更新:仅 status=0 且处理人=接收方,并发下只有一个请求能成功
        int rows = infoShareMapper.update(null, new LambdaUpdateWrapper<InfoShare>()
                .eq(InfoShare::getId, shareId)
                .eq(InfoShare::getToUserId, userId)
                .eq(InfoShare::getStatus, InfoShareStatus.PENDING.getCode())
                .set(InfoShare::getStatus, status)
                .set(InfoShare::getHandledAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_HANDLED);
        }

        if (status == 1) {
            messageService.notify(userId, share.getFromUserId(), "info_share_approved",
                    "对方已同意互换详细信息", shareId);
            eventPublisher.publishEvent(new InfoSharePushEvent(share.getFromUserId(), "approved",
                    userId, "对方已同意互换详细信息", shareId));
        } else {
            messageService.notify(userId, share.getFromUserId(), "info_share_rejected",
                    "对方拒绝了你的申请，再了解了解吧", shareId);
            eventPublisher.publishEvent(new InfoSharePushEvent(share.getFromUserId(), "rejected",
                    userId, "对方拒绝了你的申请，再了解了解吧", shareId));
        }

        log.info("Info share handled: shareId={}, status={}", shareId, status);
    }

    @Override
    public InfoShareStatusVO getShareStatus(Long userId, Long otherUserId) {
        InfoShareStatusVO vo = new InfoShareStatusVO();
        vo.setOtherUserId(otherUserId);
        User other = userMapper.selectById(otherUserId);
        if (other != null) {
            vo.setOtherNickname(other.getNickname());
            vo.setOtherAvatar(other.getAvatar());
        }

        // 任一方向已 approved 即视为已互换(不能用"最近一条"判定,否则较新的 rejected 会覆盖已通过的互换)
        InfoShare approved = infoShareMapper.selectOne(new LambdaQueryWrapper<InfoShare>()
                .and(w -> w
                        .eq(InfoShare::getFromUserId, userId).eq(InfoShare::getToUserId, otherUserId)
                        .or()
                        .eq(InfoShare::getFromUserId, otherUserId).eq(InfoShare::getToUserId, userId))
                .eq(InfoShare::getStatus, InfoShareStatus.APPROVED.getCode())
                .orderByDesc(InfoShare::getCreatedAt)
                .last("LIMIT 1"));
        if (approved != null) {
            vo.setShareId(approved.getId());
            vo.setStatus("approved");
            return vo;
        }

        // 无 approved:按最近一条 pending/rejected 显示(任一方向)
        InfoShare share = infoShareMapper.selectOne(new LambdaQueryWrapper<InfoShare>()
                .and(w -> w
                        .eq(InfoShare::getFromUserId, userId).eq(InfoShare::getToUserId, otherUserId)
                        .or()
                        .eq(InfoShare::getFromUserId, otherUserId).eq(InfoShare::getToUserId, userId))
                .orderByDesc(InfoShare::getCreatedAt)
                .last("LIMIT 1"));

        if (share == null) {
            vo.setStatus("none");
            return vo;
        }
        vo.setShareId(share.getId());
        if (share.getStatus() == InfoShareStatus.REJECTED.getCode()) {
            vo.setStatus("rejected");
        } else {
            // pending: 区分发送/接收方向
            vo.setStatus(share.getFromUserId().equals(userId) ? "pendingSent" : "pendingReceived");
        }
        return vo;
    }

    /** 单聊会话数(room_friend 按 room_key 唯一,uid1 < uid2) */
    private long countRoomFriend(Long a, Long b) {
        long uid1 = Math.min(a, b);
        long uid2 = Math.max(a, b);
        Long count = roomFriendMapper.selectCount(new LambdaQueryWrapper<RoomFriend>()
                .eq(RoomFriend::getRoomKey, uid1 + "_" + uid2));
        return count != null ? count : 0;
    }

    /** 双方实际私信条数(不依赖 room_friend,覆盖历史/种子数据里"聊过但没建会话记录"的情况) */
    private long countPrivateChat(Long a, Long b) {
        Long count = privateChatMapper.selectCount(new LambdaQueryWrapper<PrivateChat>()
                .and(w -> w.and(x -> x.eq(PrivateChat::getFromUserId, a).eq(PrivateChat::getToUserId, b))
                        .or().and(x -> x.eq(PrivateChat::getFromUserId, b).eq(PrivateChat::getToUserId, a))));
        return count != null ? count : 0;
    }

}
