package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.admin.service.DashboardService;
import com.finding.chat.entity.Report;
import com.finding.chat.mapper.ReportMapper;
import com.finding.post.entity.Appeal;
import com.finding.post.entity.Post;
import com.finding.post.mapper.AppealMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.entity.UserVerification;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserVerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据面板质量指标:性别比、认证率、留存率、审核时效(积压 + 最久待处理时长)。
 * 比率与时长计算抽为纯函数,便于单测。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final UserVerificationMapper verificationMapper;
    private final PostMapper postMapper;
    private final AppealMapper appealMapper;
    private final ReportMapper reportMapper;

    @Override
    public Map<String, Object> quality() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        long totalUsers = userMapper.selectCount(null);
        long male = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getGender, 1));
        long female = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getGender, 2));
        long approved = verificationMapper.selectCount(new LambdaQueryWrapper<UserVerification>().eq(UserVerification::getStatus, 1));

        long active7d = userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getLastLoginAt, sevenDaysAgo));
        long oldUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().lt(User::getCreatedAt, sevenDaysAgo));
        long retained = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .lt(User::getCreatedAt, sevenDaysAgo).ge(User::getLastLoginAt, sevenDaysAgo));

        long pendingPosts = postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getReviewStatus, 1));
        long pendingAppeals = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, 0));
        long pendingVerifications = verificationMapper.selectCount(new LambdaQueryWrapper<UserVerification>().eq(UserVerification::getStatus, 0));
        long pendingReports = reportMapper.selectCount(new LambdaQueryWrapper<Report>().eq(Report::getStatus, 0));

        Post oldestPost = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getReviewStatus, 1).orderByAsc(Post::getCreatedAt).last("LIMIT 1"));
        Appeal oldestAppeal = appealMapper.selectOne(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getStatus, 0).orderByAsc(Appeal::getCreatedAt).last("LIMIT 1"));
        UserVerification oldestVerification = verificationMapper.selectOne(new LambdaQueryWrapper<UserVerification>()
                .eq(UserVerification::getStatus, 0).orderByAsc(UserVerification::getCreatedAt).last("LIMIT 1"));

        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> gender = new LinkedHashMap<>();
        gender.put("male", male);
        gender.put("female", female);
        gender.put("maleRate", percent(male, male + female));
        gender.put("femaleRate", percent(female, male + female));
        result.put("genderRatio", gender);

        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("approved", approved);
        verification.put("total", totalUsers);
        verification.put("rate", percent(approved, totalUsers));
        result.put("verificationRate", verification);

        Map<String, Object> retention = new LinkedHashMap<>();
        retention.put("active7d", active7d);
        retention.put("activeRate", percent(active7d, totalUsers));
        retention.put("retained7d", retained);
        retention.put("retentionRate", percent(retained, oldUsers));
        result.put("retention", retention);

        Map<String, Object> sla = new LinkedHashMap<>();
        sla.put("pendingPosts", pendingPosts);
        sla.put("pendingAppeals", pendingAppeals);
        sla.put("pendingVerifications", pendingVerifications);
        sla.put("pendingReports", pendingReports);
        sla.put("oldestPendingPostHours", oldestPost != null ? hoursAgo(oldestPost.getCreatedAt(), now) : 0L);
        sla.put("oldestPendingAppealHours", oldestAppeal != null ? hoursAgo(oldestAppeal.getCreatedAt(), now) : 0L);
        sla.put("oldestPendingVerificationHours", oldestVerification != null ? hoursAgo(oldestVerification.getCreatedAt(), now) : 0L);
        result.put("moderationSla", sla);
        return result;
    }

    /** 百分比(保留两位小数),分母为 0 时返回 0,避免除零 */
    static double percent(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(part * 10000.0 / total) / 100.0;
    }

    /** 时间点距 now 的小时数(最久待处理时长),t 为 null 或未来时间返回 0 */
    static long hoursAgo(LocalDateTime t, LocalDateTime now) {
        if (t == null || now == null) {
            return 0;
        }
        return Math.max(0, Duration.between(t, now).toHours());
    }
}
