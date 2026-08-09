package com.finding.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.PageVO;
import com.finding.app.entity.ViewHistory;
import com.finding.app.mapper.ViewHistoryMapper;
import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 浏览记录 —— 记录 + 我最近浏览列表。
 */
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final ViewHistoryMapper viewHistoryMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    /** 记录一次浏览(每用户每目标一条,重复浏览刷新时间) */
    @PostMapping
    public Result<Void> record(@RequestBody Map<String, Object> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.ok(); // 未登录不记录

        String targetType = (String) body.get("targetType");
        Object targetIdRaw = body.get("targetId");
        if (!"post".equals(targetType) && !"user".equals(targetType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "targetType 只能是 post 或 user");
        }
        if (targetIdRaw == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "targetId 必填");
        }
        Long targetId = Long.valueOf(targetIdRaw.toString());

        ViewHistory existing = viewHistoryMapper.selectOne(
                new LambdaQueryWrapper<ViewHistory>()
                        .eq(ViewHistory::getUserId, userId)
                        .eq(ViewHistory::getTargetType, targetType)
                        .eq(ViewHistory::getTargetId, targetId));
        if (existing != null) {
            existing.setCreatedAt(LocalDateTime.now());
            viewHistoryMapper.updateById(existing);
        } else {
            ViewHistory v = new ViewHistory();
            v.setUserId(userId);
            v.setTargetType(targetType);
            v.setTargetId(targetId);
            viewHistoryMapper.insert(v);
        }
        return Result.ok();
    }

    /** 我最近浏览列表(join 动态/用户信息) */
    @GetMapping
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Page<ViewHistory> result = viewHistoryMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ViewHistory>()
                        .eq(ViewHistory::getUserId, userId)
                        .orderByDesc(ViewHistory::getCreatedAt));

        // 批量加载目标信息
        List<Long> postIds = new ArrayList<>();
        List<Long> targetUserIds = new ArrayList<>();
        for (ViewHistory h : result.getRecords()) {
            if ("post".equals(h.getTargetType())) postIds.add(h.getTargetId());
            else targetUserIds.add(h.getTargetId());
        }
        Map<Long, Post> postMap = new HashMap<>();
        Map<Long, String> authorNickMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<Post> posts = postMapper.selectBatchIds(postIds);
            for (Post p : posts) postMap.put(p.getId(), p);
            Set<Long> authorIds = new HashSet<>();
            posts.forEach(p -> authorIds.add(p.getUserId()));
            if (!authorIds.isEmpty()) {
                userMapper.selectBatchIds(authorIds)
                        .forEach(u -> authorNickMap.put(u.getId(), u.getNickname()));
            }
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!targetUserIds.isEmpty()) {
            userMapper.selectBatchIds(targetUserIds).forEach(u -> userMap.put(u.getId(), u));
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (ViewHistory h : result.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("targetType", h.getTargetType());
            map.put("targetId", h.getTargetId());
            map.put("createdAt", h.getCreatedAt());
            if ("post".equals(h.getTargetType())) {
                Post p = postMap.get(h.getTargetId());
                if (p == null) continue; // 动态已删除
                map.put("title", p.getContent());
                map.put("image", firstImage(p.getImages()));
                map.put("subtitle", authorNickMap.getOrDefault(p.getUserId(), ""));
            } else {
                User u = userMap.get(h.getTargetId());
                if (u == null) continue; // 用户不存在
                map.put("title", u.getNickname());
                map.put("image", u.getAvatar());
                map.put("subtitle", u.getSchool() != null ? u.getSchool() : "");
            }
            records.add(map);
        }

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    private String firstImage(String images) {
        if (!StringUtils.hasText(images)) return null;
        String first = images.split(",")[0];
        return StringUtils.hasText(first) ? first : null;
    }
}
