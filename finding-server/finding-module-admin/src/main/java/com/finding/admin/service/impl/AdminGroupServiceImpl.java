package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminGroupService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.group.entity.GroupChat;
import com.finding.group.entity.GroupChatMember;
import com.finding.group.entity.GroupMessage;
import com.finding.group.mapper.GroupChatMapper;
import com.finding.group.mapper.GroupChatMemberMapper;
import com.finding.group.mapper.GroupMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGroupServiceImpl implements AdminGroupService {

    private final GroupChatMapper groupMapper;
    private final GroupChatMemberMapper memberMapper;
    private final GroupMessageMapper messageMapper;
    private final AdminUserLookupService userLookupService;

    @Override
    public PageVO<Map<String, Object>> listGroups(int page, int size, String keyword) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<GroupChat> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(GroupChat::getName, keyword);
        }
        wrapper.orderByDesc(GroupChat::getCreatedAt);

        Page<GroupChat> result = groupMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量取群主昵称
        Set<Long> ownerIds = result.getRecords().stream()
                .map(GroupChat::getOwnerId).collect(Collectors.toSet());
        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(ownerIds);

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

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updateGroup(Long id, Map<String, Object> body) {
        GroupChat g = groupMapper.selectById(id);
        if (g == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        if (body.get("name") != null) g.setName((String) body.get("name"));
        if (body.get("announcement") != null) g.setAnnouncement((String) body.get("announcement"));
        groupMapper.updateById(g);
    }

    @Override
    @Transactional
    public void disbandGroup(Long id) {
        GroupChat group = groupMapper.selectById(id);
        if (group == null) throw new BusinessException(ResultCode.PARAM_ERROR, "群聊不存在");
        memberMapper.delete(new LambdaQueryWrapper<GroupChatMember>()
                .eq(GroupChatMember::getGroupId, id));
        messageMapper.delete(new LambdaQueryWrapper<GroupMessage>()
                .eq(GroupMessage::getGroupId, id));
        groupMapper.deleteById(id);
    }
}
