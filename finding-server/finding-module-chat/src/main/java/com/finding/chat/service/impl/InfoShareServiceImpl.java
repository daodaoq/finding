package com.finding.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.framework.util.InMemoryRateLimiter;
import com.finding.user.common.VerificationGuard;
import com.finding.chat.entity.InfoShare;
import com.finding.chat.mapper.InfoShareMapper;
import com.finding.message.service.MessageService;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.chat.service.InfoShareService;
import com.finding.chat.vo.InfoShareStatusVO;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;
import com.finding.user.service.UserRelationshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final WebSocketServer webSocketServer;
    private final VerificationGuard verificationGuard;
    private final UserRelationshipService relationshipService;
    private final InMemoryRateLimiter rateLimiter;

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

        InfoShare existing = infoShareMapper.selectOne(new LambdaQueryWrapper<InfoShare>()
                .eq(InfoShare::getFromUserId, fromUserId)
                .eq(InfoShare::getToUserId, toUserId));

        Long shareId;
        if (existing != null) {
            // 已存在: pending/approved 不允许重复发起; rejected 则原地改回 pending(允许重新申请)
            if (existing.getStatus() != 2) {
                throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_SENT, "已经发起过互换申请了");
            }
            existing.setStatus(0);
            existing.setHandledAt(null);
            infoShareMapper.updateById(existing);
            shareId = existing.getId();
        } else {
            InfoShare share = new InfoShare();
            share.setFromUserId(fromUserId);
            share.setToUserId(toUserId);
            share.setStatus(0);
            infoShareMapper.insert(share);
            shareId = share.getId();
        }

        // 站内通知 + WebSocket 实时推送给接收方
        User fromUser = userMapper.selectById(fromUserId);
        String nickname = fromUser != null ? fromUser.getNickname() : "有人";
        messageService.notify(fromUserId, toUserId, "info_share_request",
                nickname + " 想和你互换详细信息", shareId);
        // WS content 只放昵称,由前端弹窗拼完整文案
        pushInfoShare(toUserId, "request", fromUserId, nickname, shareId);

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
        if (share.getStatus() != 0) {
            throw new BusinessException(ResultCode.CHAT_APPLY_ALREADY_HANDLED);
        }

        share.setStatus(status);
        share.setHandledAt(LocalDateTime.now());
        infoShareMapper.updateById(share);

        if (status == 1) {
            messageService.notify(userId, share.getFromUserId(), "info_share_approved",
                    "对方已同意互换详细信息", shareId);
            pushInfoShare(share.getFromUserId(), "approved", userId, "对方已同意互换详细信息", shareId);
        } else {
            messageService.notify(userId, share.getFromUserId(), "info_share_rejected",
                    "对方拒绝了你的申请，再了解了解吧", shareId);
            pushInfoShare(share.getFromUserId(), "rejected", userId, "对方拒绝了你的申请，再了解了解吧", shareId);
        }

        log.info("Info share handled: shareId={}, status={}", shareId, status);
    }

    @Override
    public InfoShareStatusVO getShareStatus(Long userId, Long otherUserId) {
        // 找出两者之间最近一条互换记录(任一方向)
        InfoShare share = infoShareMapper.selectOne(new LambdaQueryWrapper<InfoShare>()
                .and(w -> w
                        .eq(InfoShare::getFromUserId, userId).eq(InfoShare::getToUserId, otherUserId)
                        .or()
                        .eq(InfoShare::getFromUserId, otherUserId).eq(InfoShare::getToUserId, userId))
                .orderByDesc(InfoShare::getCreatedAt)
                .last("LIMIT 1"));

        InfoShareStatusVO vo = new InfoShareStatusVO();
        vo.setOtherUserId(otherUserId);
        User other = userMapper.selectById(otherUserId);
        if (other != null) {
            vo.setOtherNickname(other.getNickname());
            vo.setOtherAvatar(other.getAvatar());
        }

        if (share == null) {
            vo.setStatus("none");
            return vo;
        }
        vo.setShareId(share.getId());
        if (share.getStatus() == 1) {
            vo.setStatus("approved");
        } else if (share.getStatus() == 2) {
            vo.setStatus("rejected");
        } else {
            // pending: 区分发送/接收方向
            vo.setStatus(share.getFromUserId().equals(userId) ? "pendingSent" : "pendingReceived");
        }
        return vo;
    }

    private void pushInfoShare(Long toUserId, String action, Long fromUserId, String content, Long shareId) {
        if (!webSocketServer.isOnline(toUserId)) return;
        WsMessage wsMsg = new WsMessage();
        wsMsg.setType("info_share");
        wsMsg.setAction(action);
        wsMsg.setFromUserId(fromUserId);
        wsMsg.setToUserId(toUserId);
        wsMsg.setContent(content);
        wsMsg.setMessageId(shareId);
        wsMsg.setTimestamp(System.currentTimeMillis());
        webSocketServer.sendToUser(toUserId, wsMsg);
    }
}
