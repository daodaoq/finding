package com.finding.admin.service.impl;

import com.finding.admin.service.AdminUserLookupService;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserLookupServiceImpl implements AdminUserLookupService {

    private final UserMapper userMapper;

    @Override
    public Map<Long, String> nicknamesByIds(Collection<Long> userIds) {
        Map<Long, String> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return result;
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : userIds) {
            if (id != null) ids.add(id);
        }
        if (ids.isEmpty()) return result;
        userMapper.selectBatchIds(ids).forEach(u -> result.put(u.getId(), u.getNickname()));
        return result;
    }

    @Override
    public String nicknameOf(Long userId) {
        if (userId == null) return null;
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getNickname();
    }
}
