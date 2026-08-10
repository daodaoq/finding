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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** 近 N 天趋势(注册/动态/搭子/活跃用户) */
    @GetMapping("/dashboard/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "7") int days) {
        int n = Math.min(Math.max(days, 3), 30);
        LocalDate today = LocalDate.now();
        List<String> dates = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> newPosts = new ArrayList<>();
        List<Long> newMates = new ArrayList<>();
        List<Long> activeUsers = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end = d.plusDays(1).atStartOfDay();
            dates.add(d.toString());
            newUsers.add(userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getCreatedAt, start).lt(User::getCreatedAt, end)));
            newPosts.add(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                    .ge(Post::getCreatedAt, start).lt(Post::getCreatedAt, end)));
            newMates.add(mateMapper.selectCount(new LambdaQueryWrapper<MateInvitation>()
                    .ge(MateInvitation::getCreatedAt, start).lt(MateInvitation::getCreatedAt, end)));
            activeUsers.add(userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getLastLoginAt, start).lt(User::getLastLoginAt, end)));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("newUsers", newUsers);
        result.put("newPosts", newPosts);
        result.put("newMates", newMates);
        result.put("activeUsers", activeUsers);
        return Result.ok(result);
    }
}
