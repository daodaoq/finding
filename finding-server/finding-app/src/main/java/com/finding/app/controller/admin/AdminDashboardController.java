package com.finding.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.Result;
import com.finding.chat.entity.Report;
import com.finding.chat.mapper.ReportMapper;
import com.finding.group.entity.GroupChat;
import com.finding.group.mapper.GroupChatMapper;
import com.finding.mate.entity.MateInvitation;
import com.finding.post.entity.Post;
import com.finding.user.entity.User;
import com.finding.user.entity.UserVerification;
import com.finding.mate.mapper.MateInvitationMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserVerificationMapper;
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
    private final ReportMapper reportMapper;
    private final GroupChatMapper groupChatMapper;

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
        stats.put("todayNewUsers", userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, LocalDateTime.now().toLocalDate())));
        stats.put("totalMates", mateMapper.selectCount(null));
        stats.put("pendingReports", reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, 0)));
        stats.put("groupCount", groupChatMapper.selectCount(null));
        return Result.ok(stats);
    }
}
