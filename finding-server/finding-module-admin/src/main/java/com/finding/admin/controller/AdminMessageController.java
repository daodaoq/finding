package com.finding.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.chat.entity.PrivateChat;
import com.finding.chat.mapper.PrivateChatMapper;
import com.finding.group.entity.GroupChat;
import com.finding.group.entity.GroupMessage;
import com.finding.group.mapper.GroupChatMapper;
import com.finding.group.mapper.GroupMessageMapper;
import com.finding.message.service.MessageService;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员 - 站内消息(系统通知)。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;
    private final UserMapper userMapper;
    private final PrivateChatMapper privateChatMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupChatMapper groupChatMapper;

    /** 给单个用户发系统通知 */
    @PostMapping("/messages")
    public Result<Void> sendMessage(@RequestBody Map<String, Object> body) {
        Long targetUserId = Long.valueOf(String.valueOf(body.get("targetUserId")));
        String content = (String) body.get("content");
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "内容不能为空");
        }
        if (userMapper.selectById(targetUserId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        messageService.notify(null, targetUserId, "system", content, null);
        return Result.ok();
    }

    /** 给全部用户广播系统通知 */
    @PostMapping("/messages/broadcast")
    public Result<Void> broadcast(@RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "内容不能为空");
        }
        userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1))
                .forEach(u -> messageService.notify(null, u.getId(), "system", content, null));
        return Result.ok();
    }

    /** 按用户查看私聊消息(内容审查),可选 otherUserId 只看两人对话 */
    @GetMapping("/messages/chat")
    public Result<PageVO<Map<String, Object>>> chatMessages(
            @RequestParam Long userId,
            @RequestParam(required = false) Long otherUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
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
        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 删除单条私聊消息 */
    @DeleteMapping("/messages/chat/{id}")
    public Result<Void> deleteChatMessage(@PathVariable Long id) {
        privateChatMapper.deleteById(id);
        return Result.ok();
    }

    /** 按群/按发送用户查看群聊消息(内容审查) */
    @GetMapping("/messages/group")
    public Result<PageVO<Map<String, Object>>> groupMessages(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
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
        Set<Long> uids = result.getRecords().stream().map(GroupMessage::getFromUserId).collect(Collectors.toSet());
        Map<Long, String> groupNameMap = new HashMap<>();
        Map<Long, String> senderNameMap = new HashMap<>();
        if (!groupIds.isEmpty()) {
            groupChatMapper.selectBatchIds(groupIds).forEach(g -> groupNameMap.put(g.getId(), g.getName()));
        }
        if (!uids.isEmpty()) {
            userMapper.selectBatchIds(uids).forEach(u -> senderNameMap.put(u.getId(), u.getNickname()));
        }

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
        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 删除单条群聊消息 */
    @DeleteMapping("/messages/group/{id}")
    public Result<Void> deleteGroupMessage(@PathVariable Long id) {
        groupMessageMapper.deleteById(id);
        return Result.ok();
    }
}
