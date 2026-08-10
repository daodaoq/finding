package com.finding.group.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.framework.websocket.WebSocketServer;
import com.finding.framework.websocket.WsMessage;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.finding.group.entity.GroupChat;
import com.finding.group.entity.GroupChatMember;
import com.finding.group.entity.GroupMessage;
import com.finding.user.entity.User;
import com.finding.user.entity.UserFollow;
import com.finding.group.mapper.GroupChatMapper;
import com.finding.group.mapper.GroupChatMemberMapper;
import com.finding.group.mapper.GroupMessageMapper;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.common.PageVO;
import com.finding.group.vo.GroupChatVO;
import com.finding.group.vo.GroupMessageVO;

@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final GroupChatMapper groupMapper;
    private final GroupChatMemberMapper memberMapper;
    private final GroupMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final WebSocketServer webSocketServer;

    /** 创建群聊 */
    @Transactional
    public GroupChatVO createGroup(Long ownerId, String name, List<Long> memberIds) {
        GroupChat group = new GroupChat();
        group.setName(name);
        group.setOwnerId(ownerId);
        group.setMemberCount(memberIds.size() + 1); // +owner
        // XSS 清洗 + 违禁词拦截
        name = XssUtil.clean(name);
        sensitiveWordFilter.assertClean(name);
        groupMapper.insert(group);

        // 添加群主
        addMember(group.getId(), ownerId, 2);
        // 添加成员
        for (Long uid : memberIds) {
            addMember(group.getId(), uid, 0);
        }

        return toVO(group, ownerId);
    }

    /** 我的群聊列表 */
    public List<GroupChatVO> listMyGroups(Long userId) {
        List<GroupChatMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<GroupChatMember>().eq(GroupChatMember::getUserId, userId));
        if (memberships.isEmpty()) return List.of();

        List<Long> groupIds = memberships.stream().map(GroupChatMember::getGroupId).toList();
        List<GroupChat> groups = groupMapper.selectBatchIds(groupIds);

        // 查每个群的最后消息
        Map<Long, GroupMessage> lastMsgMap = new HashMap<>();
        for (Long gid : groupIds) {
            List<GroupMessage> msgs = messageMapper.selectList(
                    new LambdaQueryWrapper<GroupMessage>()
                            .eq(GroupMessage::getGroupId, gid)
                            .orderByDesc(GroupMessage::getCreatedAt)
                            .last("LIMIT 1"));
            if (!msgs.isEmpty()) lastMsgMap.put(gid, msgs.get(0));
        }

        // 每个群我已读到的消息ID
        Map<Long, Long> lastReadMap = new HashMap<>();
        for (GroupChatMember m : memberships) {
            lastReadMap.put(m.getGroupId(), m.getLastReadMsgId() != null ? m.getLastReadMsgId() : 0L);
        }

        return groups.stream().map(g -> {
            GroupChatVO vo = toVO(g, userId);
            GroupMessage last = lastMsgMap.get(g.getId());
            if (last != null) {
                vo.setLastMessage("image".equals(last.getMessageType()) ? "[图片]" : last.getContent());
                vo.setLastMessageAt(last.getCreatedAt());
            }
            // 未读数 = 我未读之后且不是我自己发的
            long lastRead = lastReadMap.getOrDefault(g.getId(), 0L);
            long unread = messageMapper.selectCount(new LambdaQueryWrapper<GroupMessage>()
                    .eq(GroupMessage::getGroupId, g.getId())
                    .gt(GroupMessage::getId, lastRead)
                    .ne(GroupMessage::getFromUserId, userId));
            vo.setUnreadCount((int) unread);
            return vo;
        }).collect(Collectors.toList());
    }

    /** 用户打开群聊后标记已读:last_read_msg_id = 群当前最大消息ID */
    @Transactional
    public void markRead(Long groupId, Long userId) {
        GroupChatMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupChatMember>()
                .eq(GroupChatMember::getGroupId, groupId)
                .eq(GroupChatMember::getUserId, userId));
        if (member == null) {
            return;
        }
        GroupMessage maxMsg = messageMapper.selectOne(new LambdaQueryWrapper<GroupMessage>()
                .eq(GroupMessage::getGroupId, groupId)
                .orderByDesc(GroupMessage::getId)
                .last("LIMIT 1"));
        long maxId = maxMsg != null ? maxMsg.getId() : 0L;
        if (member.getLastReadMsgId() == null || member.getLastReadMsgId() < maxId) {
            member.setLastReadMsgId(maxId);
            memberMapper.updateById(member);
        }
    }

    /** 群详情 */
    public GroupChatVO getGroupDetail(Long groupId, Long userId) {
        GroupChat group = groupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        GroupChatVO vo = toVO(group, userId);

        // 加载成员列表
        List<GroupChatMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<GroupChatMember>().eq(GroupChatMember::getGroupId, groupId));
        List<Long> uids = members.stream().map(GroupChatMember::getUserId).toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!uids.isEmpty()) {
            userMapper.selectBatchIds(uids).forEach(u -> userMap.put(u.getId(), u));
        }
        vo.setMembers(members.stream().map(m -> {
            GroupChatVO.GroupMemberVO mv = new GroupChatVO.GroupMemberVO();
            mv.setUserId(m.getUserId());
            mv.setRole(m.getRole());
            User u = userMap.get(m.getUserId());
            if (u != null) { mv.setNickname(u.getNickname()); mv.setAvatar(u.getAvatar()); }
            return mv;
        }).toList());

        return vo;
    }

    /** 发送群消息 */
    public GroupMessageVO sendMessage(Long groupId, Long fromUserId, String content, String messageType) {
        GroupMessage msg = new GroupMessage();
        msg.setGroupId(groupId);
        msg.setFromUserId(fromUserId);
        msg.setContent(content);
        msg.setMessageType(messageType != null ? messageType : "text");
        // XSS 清洗 + 违禁词拦截
        content = XssUtil.clean(content);
        sensitiveWordFilter.assertClean(content);
        messageMapper.insert(msg);

        User u = userMapper.selectById(fromUserId);

        // WS 实时推送给群内所有成员(含发送者本人,前端按 messageId 去重/替换临时消息)
        WsMessage ws = new WsMessage();
        ws.setType("group_chat");
        ws.setMessageId(msg.getId());
        ws.setConversationId(groupId);
        ws.setFromUserId(fromUserId);
        ws.setFromUserNickname(u != null ? u.getNickname() : "");
        ws.setFromUserAvatar(u != null ? u.getAvatar() : "");
        ws.setContent(content);
        ws.setMessageType(messageType != null ? messageType : "text");
        ws.setTimestamp(System.currentTimeMillis());
        memberMapper.selectList(new LambdaQueryWrapper<GroupChatMember>()
                        .eq(GroupChatMember::getGroupId, groupId))
                .forEach(m -> webSocketServer.sendToUser(m.getUserId(), ws));

        GroupMessageVO vo = new GroupMessageVO();
        vo.setId(msg.getId());
        vo.setGroupId(groupId);
        vo.setFromUserId(fromUserId);
        vo.setFromUserNickname(u != null ? u.getNickname() : "");
        vo.setFromUserAvatar(u != null ? u.getAvatar() : "");
        vo.setContent(content);
        vo.setMessageType(messageType);
        vo.setIsRecalled(msg.getIsRecalled());
        vo.setCreatedAt(msg.getCreatedAt());
        return vo;
    }

    /** 群消息历史 */
    public PageVO<GroupMessageVO> getMessageHistory(Long groupId, int page, int size) {
        Page<GroupMessage> pg = new Page<>(page, size);
        Page<GroupMessage> result = messageMapper.selectPage(pg,
                new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, groupId)
                        .orderByAsc(GroupMessage::getCreatedAt));

        List<Long> uids = result.getRecords().stream().map(GroupMessage::getFromUserId).distinct().toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!uids.isEmpty()) {
            userMapper.selectBatchIds(uids).forEach(u -> userMap.put(u.getId(), u));
        }

        List<GroupMessageVO> records = result.getRecords().stream().map(m -> {
            GroupMessageVO vo = new GroupMessageVO();
            vo.setId(m.getId());
            vo.setGroupId(m.getGroupId());
            vo.setFromUserId(m.getFromUserId());
            User uu = userMap.get(m.getFromUserId());
            vo.setFromUserNickname(uu != null ? uu.getNickname() : "");
            vo.setFromUserAvatar(uu != null ? uu.getAvatar() : "");
            vo.setContent(m.getContent());
            vo.setMessageType(m.getMessageType());
            vo.setIsRecalled(m.getIsRecalled());
            vo.setCreatedAt(m.getCreatedAt());
            return vo;
        }).toList();

        return PageVO.of(records, result.getTotal(), page, size);
    }

    /** 可邀请的用户：正在关注的人 + 最近私聊的人，排除已在群内的 */
    public List<Map<String, Object>> getInvitableUsers(Long userId, Long groupId) {
        Set<Long> existing = new HashSet<>();
        if (groupId != null) {
            memberMapper.selectList(new LambdaQueryWrapper<GroupChatMember>()
                    .eq(GroupChatMember::getGroupId, groupId))
                    .forEach(m -> existing.add(m.getUserId()));
        }

        Set<Long> candidates = new LinkedHashSet<>();
        // 关注的人
        followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId))
                .forEach(f -> candidates.add(f.getFolloweeId()));
        // 最近私聊的人（从 contact 表）
        // 简化：加上最近联系过的用户

        candidates.remove(userId);
        candidates.removeAll(existing);

        if (candidates.isEmpty()) return List.of();

        Map<Long, User> userMap = new HashMap<>();
        userMapper.selectBatchIds(candidates).forEach(u -> userMap.put(u.getId(), u));

        return candidates.stream()
                .map(uid -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    User u = userMap.get(uid);
                    m.put("userId", uid);
                    m.put("nickname", u != null ? u.getNickname() : "");
                    m.put("avatar", u != null ? u.getAvatar() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── public mutators ──

    @Transactional
    public void addMembers(Long operatorId, Long groupId, List<Long> userIds) {
        GroupChat group = groupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        for (Long uid : userIds) {
            if (memberMapper.selectCount(new LambdaQueryWrapper<GroupChatMember>()
                    .eq(GroupChatMember::getGroupId, groupId)
                    .eq(GroupChatMember::getUserId, uid)) == 0) {
                addMember(groupId, uid, 0);
            }
        }
        group.setMemberCount(memberMapper.selectCount(
                new LambdaQueryWrapper<GroupChatMember>().eq(GroupChatMember::getGroupId, groupId)).intValue());
        groupMapper.updateById(group);
    }

    @Transactional
    public void removeMember(Long operatorId, Long groupId, Long targetUserId) {
        GroupChat group = groupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        if (!group.getOwnerId().equals(operatorId))
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅群主可移除成员");
        if (targetUserId.equals(operatorId))
            throw new BusinessException(ResultCode.PARAM_ERROR, "群主不能移除自己，请使用退出群聊");
        memberMapper.delete(new LambdaQueryWrapper<GroupChatMember>()
                .eq(GroupChatMember::getGroupId, groupId)
                .eq(GroupChatMember::getUserId, targetUserId));
        group.setMemberCount(Math.max(1, group.getMemberCount() - 1));
        groupMapper.updateById(group);
    }

    @Transactional
    public void leaveOrDisband(Long userId, Long groupId) {
        GroupChat group = groupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        if (group.getOwnerId().equals(userId)) {
            // 群主解散
            memberMapper.delete(new LambdaQueryWrapper<GroupChatMember>()
                    .eq(GroupChatMember::getGroupId, groupId));
            groupMapper.deleteById(groupId);
        } else {
            // 成员退出
            memberMapper.delete(new LambdaQueryWrapper<GroupChatMember>()
                    .eq(GroupChatMember::getGroupId, groupId)
                    .eq(GroupChatMember::getUserId, userId));
            group.setMemberCount(Math.max(1, group.getMemberCount() - 1));
            groupMapper.updateById(group);
        }
    }

    /** 群主修改群公告 */
    @Transactional
    public void updateAnnouncement(Long groupId, Long userId, String announcement) {
        GroupChat group = groupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        if (!group.getOwnerId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅群主可修改群公告");
        }
        group.setAnnouncement(XssUtil.clean(announcement));
        groupMapper.updateById(group);
    }

    /** 撤回群消息(发送者本人,2分钟内),并 WS 通知群内成员 */
    @Transactional
    public void recallMessage(Long groupId, Long userId, Long messageId) {
        GroupMessage msg = messageMapper.selectById(messageId);
        if (msg == null || (msg.getIsRecalled() != null && msg.getIsRecalled() == 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息不存在或已撤回");
        }
        if (!msg.getGroupId().equals(groupId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息不属于该群");
        }
        if (!msg.getFromUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能撤回自己发送的消息");
        }
        if (msg.getCreatedAt() != null
                && msg.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "发送超过 2 分钟的消息无法撤回");
        }
        GroupMessage update = new GroupMessage();
        update.setId(messageId);
        update.setIsRecalled(1);
        // 保留原 content,仅标记撤回,便于管理员审计原文
        messageMapper.updateById(update);

        // WS 通知群内所有成员刷新该消息
        WsMessage ws = new WsMessage();
        ws.setType("message_recalled");
        ws.setAction("group");
        ws.setMessageId(messageId);
        ws.setConversationId(groupId);
        ws.setTimestamp(System.currentTimeMillis());
        memberMapper.selectList(new LambdaQueryWrapper<GroupChatMember>()
                        .eq(GroupChatMember::getGroupId, groupId))
                .forEach(m -> webSocketServer.sendToUser(m.getUserId(), ws));
    }

    // ── private ──

    private void addMember(Long groupId, Long userId, int role) {
        GroupChatMember m = new GroupChatMember();
        m.setGroupId(groupId);
        m.setUserId(userId);
        m.setRole(role);
        memberMapper.insert(m);
    }

    private GroupChatVO toVO(GroupChat g, Long userId) {
        GroupChatVO vo = new GroupChatVO();
        vo.setId(g.getId());
        vo.setName(g.getName());
        vo.setAvatar(g.getAvatar());
        vo.setOwnerId(g.getOwnerId());
        vo.setMemberCount(g.getMemberCount());
        vo.setAnnouncement(g.getAnnouncement());
        vo.setCreatedAt(g.getCreatedAt());
        return vo;
    }
}
