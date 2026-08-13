package com.finding.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.PageVO;
import com.finding.group.entity.GroupChat;
import com.finding.group.entity.GroupChatMember;
import com.finding.group.entity.GroupMessage;
import com.finding.group.mapper.GroupChatMapper;
import com.finding.group.mapper.GroupChatMemberMapper;
import com.finding.group.mapper.GroupMessageMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 群聊管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminGroupController {

    private final GroupChatMapper groupMapper;
    private final GroupChatMemberMapper memberMapper;
    private final GroupMessageMapper messageMapper;
    private final UserMapper userMapper;

    @GetMapping("/groups")
    public Result<PageVO<Map<String, Object>>> listGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<GroupChat> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(GroupChat::getName, keyword);
        }
        wrapper.orderByDesc(GroupChat::getCreatedAt);

        Page<GroupChat> result = groupMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量取群主昵称
        Set<Long> ownerIds = result.getRecords().stream()
                .map(GroupChat::getOwnerId).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!ownerIds.isEmpty()) {
            userMapper.selectBatchIds(ownerIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(g -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", g.getId());
            map.put("name", g.getName());
            map.put("ownerId", g.getOwnerId());
            map.put("ownerNickname", nicknameMap.getOrDefault(g.getOwnerId(), "用户" + g.getOwnerId()));
            map.put("memberCount", g.getMemberCount());
            map.put("avatar", g.getAvatar());
            map.put("createdAt", g.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 编辑群信息(管理员可改一切) */
    @PutMapping("/groups/{id}")
    public Result<Void> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        GroupChat g = groupMapper.selectById(id);
        if (g == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        if (body.get("name") != null) g.setName((String) body.get("name"));
        if (body.get("announcement") != null) g.setAnnouncement((String) body.get("announcement"));
        groupMapper.updateById(g);
        return Result.ok();
    }

    /** 解散群：级联删除成员关系、群消息、群本身 */
    @DeleteMapping("/groups/{id}")
    @Transactional
    public Result<Void> disbandGroup(@PathVariable Long id) {
        GroupChat group = groupMapper.selectById(id);
        if (group == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        memberMapper.delete(new LambdaQueryWrapper<GroupChatMember>()
                .eq(GroupChatMember::getGroupId, id));
        messageMapper.delete(new LambdaQueryWrapper<GroupMessage>()
                .eq(GroupMessage::getGroupId, id));
        groupMapper.deleteById(id);
        return Result.ok();
    }
}
