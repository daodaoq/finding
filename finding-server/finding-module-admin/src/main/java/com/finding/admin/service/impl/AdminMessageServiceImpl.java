package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminMessageService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.group.entity.GroupChat;
import com.finding.group.entity.GroupMessage;
import com.finding.group.mapper.GroupChatMapper;
import com.finding.group.mapper.GroupMessageMapper;
import com.finding.message.service.MessageService;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMessageServiceImpl implements AdminMessageService {

    private final MessageService messageService;
    private final UserMapper userMapper;
    private final PrivateChatMapper privateChatMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupChatMapper groupChatMapper;
    private final AdminUserLookupService userLookupService;
    private final OperationAuditService operationAuditService;

    @Override
    public void sendMessage(Map<String, Object> body) {
        Long targetUserId = asLong(body.get("targetUserId"), "targetUserId");
        String content = asString(body.get("content"));
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "内容不能为空");
        }
        if (userMapper.selectById(targetUserId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        messageService.notify(null, targetUserId, "system", content, null);
    }

    @Override
    public void broadcast(Map<String, Object> body) {
        String content = asString(body.get("content"));
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "内容不能为空");
        }
        userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1))
                .forEach(u -> messageService.notify(null, u.getId(), "system", content, null));
    }

    @Override
    public PageVO<Map<String, Object>> chatMessages(Long userId, Long otherUserId, int page, int size) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<PrivateChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(PrivateChat::getFromUserId, userId)
                .or().eq(PrivateChat::getToUserId, userId));
        if (otherUserId != null) {
            wrapper.and(w -> w.and(x -> x.eq(PrivateChat::getFromUserId, userId).eq(PrivateChat::getToUserId, otherUserId))
                    .or(x -> x.eq(PrivateChat::getFromUserId, otherUserId).eq(PrivateChat::getToUserId, userId)));
        }
        wrapper.orderByDesc(PrivateChat::getCreatedAt);
        Page<PrivateChat> result = privateChatMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = result.getRecords().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("fromUserId", m.getFromUserId());
            map.put("toUserId", m.getToUserId());
            map.put("messageType", m.getMessageType());
            map.put("isRecalled", m.getIsRecalled());
            // 保留原文,撤回的在前端以标记展示,方便审计
            map.put("content", m.getContent());
            map.put("createdAt", m.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void deleteChatMessage(Long adminId, Long id) {
        if (id == null || privateChatMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息不存在");
        }
        privateChatMapper.deleteById(id);
        operationAuditService.record(adminId, "chat_message_delete", "private_chat", id, "删除私聊消息", null);
    }

    @Override
    public PageVO<Map<String, Object>> groupMessages(Long groupId, Long userId, int page, int size) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<GroupMessage> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) {
            wrapper.eq(GroupMessage::getGroupId, groupId);
        }
        if (userId != null) {
            wrapper.eq(GroupMessage::getFromUserId, userId);
        }
        wrapper.orderByDesc(GroupMessage::getCreatedAt);
        Page<GroupMessage> result = groupMessageMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> groupIds = result.getRecords().stream().map(GroupMessage::getGroupId).collect(Collectors.toSet());
        Map<Long, String> groupNameMap = new java.util.HashMap<>();
        if (!groupIds.isEmpty()) {
            groupChatMapper.selectBatchIds(groupIds).forEach(g -> groupNameMap.put(g.getId(), g.getName()));
        }
        Map<Long, String> senderNameMap = userLookupService.nicknamesByIds(
                result.getRecords().stream().map(GroupMessage::getFromUserId).collect(Collectors.toSet()));

        List<Map<String, Object>> records = result.getRecords().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("groupId", m.getGroupId());
            map.put("groupName", groupNameMap.getOrDefault(m.getGroupId(), ""));
            map.put("fromUserId", m.getFromUserId());
            map.put("senderName", senderNameMap.getOrDefault(m.getFromUserId(), ""));
            map.put("messageType", m.getMessageType());
            map.put("isRecalled", m.getIsRecalled());
            map.put("content", m.getContent());
            map.put("createdAt", m.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void deleteGroupMessage(Long adminId, Long id) {
        if (id == null || groupMessageMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "消息不存在");
        }
        groupMessageMapper.deleteById(id);
        operationAuditService.record(adminId, "group_message_delete", "group_message", id, "删除群聊消息", null);
    }

    // ── 内部工具 ──

    /** 显式解析必填 id:缺参时报参数错误,而不是把 "null" 当成 id 解析后抛 500 */
    private Long asLong(Object value, String field) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, field + " 不能为空");
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, field + " 格式不正确");
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
