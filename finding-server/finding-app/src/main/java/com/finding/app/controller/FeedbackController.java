package com.finding.app.controller;

import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.util.XssUtil;
import com.finding.app.entity.Feedback;
import com.finding.app.mapper.FeedbackMapper;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端 - 意见反馈 / 客服工单提交。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackMapper feedbackMapper;

    @PostMapping("/feedback")
    public Result<Void> submit(@RequestBody Map<String, String> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        String content = body.get("content");
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "反馈内容不能为空");
        }
        Feedback f = new Feedback();
        f.setUserId(userId);
        f.setType(body.getOrDefault("type", "other"));
        f.setContent(XssUtil.clean(content));
        f.setContact(body.get("contact"));
        f.setStatus(0);
        feedbackMapper.insert(f);
        return Result.ok();
    }
}
