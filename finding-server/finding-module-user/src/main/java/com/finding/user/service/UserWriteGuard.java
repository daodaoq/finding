package com.finding.user.service;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 禁言(冻结)守卫 —— 冻结用户(status=2)只读,禁止发布内容。
 * 所有内容写入口(发动态/评论/搭子/私信/聊天申请)落库前调用。
 */
@Component
@RequiredArgsConstructor
public class UserWriteGuard {

    private final UserMapper userMapper;

    public void checkWritable(Long userId) {
        if (userId == null) return;
        User user = userMapper.selectById(userId);
        if (user != null && user.getStatus() != null && user.getStatus() == 2) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
    }
}
