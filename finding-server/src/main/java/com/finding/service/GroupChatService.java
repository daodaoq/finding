package com.finding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.entity.*;
import com.finding.mapper.*;
import com.finding.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final GroupChatMapper groupMapper;
    private final GroupChatMemberMapper memberMapper;
    private final GroupMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;

    /** 创建群聊 */
    @Transactional
    public GroupChatVO createGroup(Long ownerId, String name, List<Long> memberIds) {
        GroupChat group = new GroupChat();
        group.setName(name);
        group.setOwnerId(ownerId);
        group.setMemberCount(memberIds.size() + 1); // +owner
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

        return groups.stream().map(g -> {
            GroupChatVO vo = toVO(g, userId);
            GroupMessage last = lastMsgMap.get(g.getId());
            if (last != null) {
                vo.setLastMessage(last.getContent());
                vo.setLastMessageAt(last.getCreatedAt());
            }
            return vo;
        }).collect(Collectors.toList());
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
        messageMapper.insert(msg);

        User u = userMapper.selectById(fromUserId);
        GroupMessageVO vo = new GroupMessageVO();
        vo.setId(msg.getId());
        vo.setGroupId(groupId);
        vo.setFromUserId(fromUserId);
        vo.setFromUserNickname(u != null ? u.getNickname() : "");
        vo.setFromUserAvatar(u != null ? u.getAvatar() : "");
        vo.setContent(content);
        vo.setMessageType(messageType);
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
