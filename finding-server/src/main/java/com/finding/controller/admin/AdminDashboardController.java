package com.finding.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.Result;
import com.finding.entity.MateInvitation;
import com.finding.entity.Post;
import com.finding.entity.UserVerification;
import com.finding.mapper.MateInvitationMapper;
import com.finding.mapper.PostMapper;
import com.finding.mapper.UserMapper;
import com.finding.mapper.UserVerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员 - 数据面板。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final MateInvitationMapper mateMapper;
    private final UserVerificationMapper verificationMapper;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("todayPosts", postMapper.selectCount(
                new LambdaQueryWrapper<Post>().ge(Post::getCreatedAt, LocalDateTime.now().toLocalDate())));
        stats.put("todayMates", mateMapper.selectCount(
                new LambdaQueryWrapper<MateInvitation>().ge(MateInvitation::getCreatedAt, LocalDateTime.now().toLocalDate())));
        stats.put("pendingVerifications", verificationMapper.selectCount(
                new LambdaQueryWrapper<UserVerification>().eq(UserVerification::getStatus, 0)));
        return Result.ok(stats);
    }
}
