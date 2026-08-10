package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.event.UserBlockedEvent;
import com.finding.user.entity.User;
import com.finding.user.entity.UserBlock;
import com.finding.user.mapper.UserBlockMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockMapper userBlockMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void block(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能拉黑自己");
        }
        if (userMapper.selectById(targetUserId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        Long count = userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .eq(UserBlock::getBlockedUserId, targetUserId));
        if (count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已拉黑该用户");
        }
        UserBlock block = new UserBlock();
        block.setUserId(userId);
        block.setBlockedUserId(targetUserId);
        userBlockMapper.insert(block);
        // 拉黑联动:通知各业务模块(如取消双方待处理聊天申请)
        eventPublisher.publishEvent(new UserBlockedEvent(userId, targetUserId));
    }

    @Override
    public void unblock(Long userId, Long targetUserId) {
        userBlockMapper.delete(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .eq(UserBlock::getBlockedUserId, targetUserId));
    }

    @Override
    public boolean isBlocked(Long userId, Long targetUserId) {
        return userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .eq(UserBlock::getBlockedUserId, targetUserId)) > 0;
    }

    @Override
    public Map<String, Boolean> blockStatus(Long userId, Long targetUserId) {
        Map<String, Boolean> map = new HashMap<>();
        map.put("blocked", isBlocked(userId, targetUserId));
        map.put("blockedBy", isBlocked(targetUserId, userId));
        return map;
    }
}
