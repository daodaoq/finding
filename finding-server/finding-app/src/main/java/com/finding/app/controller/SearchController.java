package com.finding.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.Result;
import com.finding.mate.service.MateService;
import com.finding.post.entity.Post;
import com.finding.user.entity.User;
import com.finding.user.entity.UserSettings;
import com.finding.post.mapper.PostMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserSettingsMapper;
import com.finding.user.security.JwtInterceptor;
import com.finding.user.service.UserRelationshipService;
import com.finding.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final MateService mateService;
    private final UserSettingsMapper userSettingsMapper;
    private final UserRelationshipService relationshipService;

    @GetMapping
    public Result<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String timeRange) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (!StringUtils.hasText(keyword)) {
            result.put("users", PageVO.of(List.of(), 0L, page, size));
            result.put("posts", PageVO.of(List.of(), 0L, page, size));
            result.put("mates", PageVO.of(List.of(), 0L, page, size));
            return Result.ok(result);
        }

        String kw = "%" + keyword + "%";

        // 用户：仅按昵称模糊匹配(不含手机号,防止未登录按手机号枚举注册用户;排除关闭"允许被搜索"、自己、双向拉黑)
        Long currentUserId = JwtInterceptor.getCurrentUserId();
        List<Long> hiddenIds = userSettingsMapper.selectList(
                        new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getSearchable, 0))
                .stream().map(UserSettings::getUserId).toList();
        Set<Long> blockedIds = currentUserId != null ? relationshipService.blockedUserIds(currentUserId) : Set.of();
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .ne(currentUserId != null, User::getId, currentUserId)
                        .notIn(!hiddenIds.isEmpty(), User::getId, hiddenIds)
                        .notIn(!blockedIds.isEmpty(), User::getId, blockedIds)
                        .like(User::getNickname, keyword)
                        .orderByDesc(User::getLastLoginAt));
        List<Map<String, Object>> users = userPage.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar());
            m.put("school", u.getSchool());
            m.put("signature", relationshipService.canViewDetailedProfile(currentUserId, u.getId()) ? u.getSignature() : null);
            return m;
        }).toList();

        // 动态：内容 + 标签模糊匹配;支持分类/学校/时间范围过滤
        LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getReviewStatus, 0)
                .and(w -> w.like(Post::getContent, keyword).or().like(Post::getTags, keyword))
                .eq(StringUtils.hasText(category), Post::getCategory, category);
        if (StringUtils.hasText(school)) {
            List<Long> schoolUserIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .eq(User::getStatus, 1)
                            .like(User::getSchool, school))
                    .stream().map(User::getId).toList();
            if (schoolUserIds.isEmpty()) {
                postWrapper.apply("1 = 0");
            } else {
                postWrapper.in(Post::getUserId, schoolUserIds);
            }
        }
        if (StringUtils.hasText(timeRange)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime since = switch (timeRange) {
                case "day" -> now.minusDays(1);
                case "week" -> now.minusDays(7);
                case "month" -> now.minusDays(30);
                default -> null;
            };
            if (since != null) postWrapper.ge(Post::getCreatedAt, since);
        }
        postWrapper.orderByDesc(Post::getIsTop).orderByDesc(Post::getCreatedAt);
        Page<Post> postPage = postMapper.selectPage(new Page<>(page, size), postWrapper);
        List<Long> postUserIds = postPage.getRecords().stream().map(Post::getUserId).distinct().toList();
        Map<Long, User> postUserMap = new HashMap<>();
        if (!postUserIds.isEmpty()) {
            userMapper.selectBatchIds(postUserIds).forEach(u -> postUserMap.put(u.getId(), u));
        }
        List<Map<String, Object>> posts = postPage.getRecords().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("content", p.getContent());
            m.put("userId", p.getUserId());
            m.put("category", p.getCategory());
            m.put("tags", p.getTags() != null ? List.of(p.getTags().split(",")) : List.of());
            User uu = postUserMap.get(p.getUserId());
            m.put("userNickname", uu != null ? uu.getNickname() : "");
            m.put("userAvatar", uu != null ? uu.getAvatar() : "");
            m.put("likeCount", p.getLikeCount());
            m.put("commentCount", p.getCommentCount());
            m.put("createdAt", p.getCreatedAt());
            return m;
        }).toList();

        // 搭子:按标题模糊匹配(复用公开可见性过滤:进行中/已发布/未过期/排除拉黑;匿名不返回发起人)
        PageVO<Map<String, Object>> matePage = mateService.searchInvitations(currentUserId, keyword, page, size);

        result.put("users", PageVO.of(users, userPage.getTotal(), page, size));
        result.put("posts", PageVO.of(posts, postPage.getTotal(), page, size));
        result.put("mates", matePage);

        return Result.ok(result);
    }
}
