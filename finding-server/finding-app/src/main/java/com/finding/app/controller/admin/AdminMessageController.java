package com.finding.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.message.service.MessageService;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 站内消息(系统通知)。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;
    private final UserMapper userMapper;

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
}
