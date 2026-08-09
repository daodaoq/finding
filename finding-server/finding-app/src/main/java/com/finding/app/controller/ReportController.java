package com.finding.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.entity.Report;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.chat.mapper.ReportMapper;
import com.finding.group.entity.GroupChat;
import com.finding.group.entity.GroupMessage;
import com.finding.group.mapper.GroupChatMapper;
import com.finding.group.mapper.GroupMessageMapper;
import com.finding.mate.entity.MateInvitation;
import com.finding.mate.mapper.MateInvitationMapper;
import com.finding.post.entity.Post;
import com.finding.post.entity.PostComment;
import com.finding.post.mapper.PostCommentMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.entity.UserResume;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserResumeMapper;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一投诉接口 —— 记录被投诉的具体内容快照,便于管理员判断。
 * targetType: message/post/comment/user/resume
 */
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportMapper reportMapper;
    private final PrivateChatMapper privateChatMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupChatMapper groupChatMapper;
    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final MateInvitationMapper mateInvitationMapper;
    private final UserMapper userMapper;
    private final UserResumeMapper resumeMapper;

    @PostMapping
    public Result<Void> report(@RequestBody Map<String, Object> body) {
        Long fromUserId = JwtInterceptor.getCurrentUserId();
        String targetType = body.get("targetType") != null ? body.get("targetType").toString() : null;
        Long targetId = body.get("targetId") != null ? Long.valueOf(body.get("targetId").toString()) : null;
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        Long roomId = body.get("roomId") != null ? Long.valueOf(body.get("roomId").toString()) : null;

        if (!StringUtils.hasText(targetType)) throw new BusinessException(ResultCode.PARAM_ERROR, "投诉类型必填");
        if (targetId == null) throw new BusinessException(ResultCode.PARAM_ERROR, "targetId 必填");
        if (!StringUtils.hasText(reason)) throw new BusinessException(ResultCode.PARAM_ERROR, "请选择投诉原因");

        Long targetUserId;
        String snapshot;
        switch (targetType) {
            case "message" -> {
                PrivateChat m = privateChatMapper.selectById(targetId);
                if (m != null) {
                    targetUserId = m.getFromUserId();
                    snapshot = "[私聊消息] " + (m.getContent() != null ? m.getContent() : "");
                    if (roomId == null) roomId = m.getRoomId();
                } else {
                    GroupMessage gm = groupMessageMapper.selectById(targetId);
                    if (gm == null) throw new BusinessException(ResultCode.PARAM_ERROR, "消息不存在");
                    targetUserId = gm.getFromUserId();
                    snapshot = "[群聊消息] " + (gm.getContent() != null ? gm.getContent() : "");
                }
            }
            case "post" -> {
                Post p = postMapper.selectById(targetId);
                if (p == null || p.getStatus() == 0) throw new BusinessException(ResultCode.POST_NOT_FOUND);
                targetUserId = p.getUserId();
                snapshot = "[动态] " + (p.getContent() != null ? p.getContent() : "");
            }
            case "comment" -> {
                PostComment c = commentMapper.selectById(targetId);
                if (c == null) throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
                targetUserId = c.getUserId();
                snapshot = "[评论] " + (c.getContent() != null ? c.getContent() : "");
            }
            case "mate" -> {
                MateInvitation m = mateInvitationMapper.selectById(targetId);
                if (m == null) throw new BusinessException(ResultCode.MATE_NOT_FOUND);
                targetUserId = m.getUserId();
                snapshot = "[搭子邀约] 标题:" + (m.getTitle() != null ? m.getTitle() : "")
                        + " 描述:" + (m.getDescription() != null ? m.getDescription() : "");
            }
            case "group" -> {
                GroupChat g = groupChatMapper.selectById(targetId);
                if (g == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
                targetUserId = g.getOwnerId();
                snapshot = "[群聊] 名称:" + (g.getName() != null ? g.getName() : "")
                        + " 公告:" + (g.getAnnouncement() != null ? g.getAnnouncement() : "");
            }
            case "user", "resume" -> {
                User u = userMapper.selectById(targetId);
                if (u == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
                targetUserId = targetId;
                snapshot = buildUserSnapshot(targetId, u);
            }
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的投诉类型");
        }

        if (targetUserId != null && targetUserId.equals(fromUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能投诉自己");
        }

        Report r = new Report();
        r.setFromUserId(fromUserId);
        r.setTargetUserId(targetUserId);
        r.setRoomId(roomId);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setContentSnapshot(snapshot);
        r.setReason(reason);
        r.setStatus(0);
        reportMapper.insert(r);
        return Result.ok();
    }

    /** 用户/情感卡片的资料快照 */
    private String buildUserSnapshot(Long userId, User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("昵称:").append(u.getNickname());
        if (u.getSchool() != null) sb.append(" 学校:").append(u.getSchool());
        if (u.getSignature() != null && !u.getSignature().isEmpty()) sb.append("\n签名:").append(u.getSignature());
        UserResume res = resumeMapper.selectOne(
                new LambdaQueryWrapper<UserResume>().eq(UserResume::getUserId, userId));
        if (res != null) {
            appendIf(sb, "职业", res.getCareer());
            appendIf(sb, "性格特质", res.getPersonalityTraits());
            appendIf(sb, "个人标签", res.getPersonalTags());
            appendIf(sb, "理想的另一半·硬性条件", res.getHardConditions());
            appendIf(sb, "走心宣言", res.getLoveExpectation());
        }
        return sb.toString();
    }

    private void appendIf(StringBuilder sb, String label, String val) {
        if (val != null && !val.isEmpty()) {
            sb.append("\n").append(label).append(":").append(val);
        }
    }
}
