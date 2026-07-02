package com.finding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.Result;
import com.finding.entity.MateInvitation;
import com.finding.entity.Post;
import com.finding.entity.User;
import com.finding.mapper.MateInvitationMapper;
import com.finding.mapper.PostMapper;
import com.finding.mapper.UserMapper;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final MateInvitationMapper mateMapper;

    @GetMapping
    public Result<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (!StringUtils.hasText(keyword)) {
            result.put("users", PageVO.of(List.of(), 0L, page, size));
            result.put("posts", PageVO.of(List.of(), 0L, page, size));
            result.put("mates", PageVO.of(List.of(), 0L, page, size));
            return Result.ok(result);
        }

        String kw = "%" + keyword + "%";

        // 用户：按昵称或手机号模糊匹配
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword)
                        .orderByDesc(User::getLastLoginAt));
        List<Map<String, Object>> users = userPage.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar());
            m.put("school", u.getSchool());
            m.put("signature", u.getSignature());
            return m;
        }).toList();

        // 动态：按内容模糊匹配
        Page<Post> postPage = postMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Post>()
                        .like(Post::getContent, keyword)
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getCreatedAt));
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
            User uu = postUserMap.get(p.getUserId());
            m.put("userNickname", uu != null ? uu.getNickname() : "");
            m.put("userAvatar", uu != null ? uu.getAvatar() : "");
            m.put("likeCount", p.getLikeCount());
            m.put("commentCount", p.getCommentCount());
            m.put("createdAt", p.getCreatedAt());
            return m;
        }).toList();

        // 搭子：按标题模糊匹配
        Page<MateInvitation> matePage = mateMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MateInvitation>()
                        .like(MateInvitation::getTitle, keyword)
                        .orderByDesc(MateInvitation::getCreatedAt));
        List<Map<String, Object>> mates = matePage.getRecords().stream().map(mv -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", mv.getId());
            m.put("title", mv.getTitle());
            m.put("category", mv.getCategory());
            m.put("location", mv.getLocation());
            m.put("activityTime", mv.getActivityTime());
            m.put("createdAt", mv.getCreatedAt());
            return m;
        }).toList();

        result.put("users", PageVO.of(users, userPage.getTotal(), page, size));
        result.put("posts", PageVO.of(posts, postPage.getTotal(), page, size));
        result.put("mates", PageVO.of(mates, matePage.getTotal(), page, size));

        return Result.ok(result);
    }
}
