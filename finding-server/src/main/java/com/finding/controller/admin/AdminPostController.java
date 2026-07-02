package com.finding.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.entity.Post;
import com.finding.mapper.PostMapper;
import com.finding.mapper.UserMapper;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 动态管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostMapper postMapper;
    private final UserMapper userMapper;

    @GetMapping("/posts")
    public Result<PageVO<Map<String, Object>>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Post::getContent, keyword).or().eq(Post::getId, keyword));
        }
        wrapper.orderByDesc(Post::getCreatedAt);

        Page<Post> result = postMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> userIds = new HashSet<>();
        result.getRecords().forEach(p -> userIds.add(p.getUserId()));
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("content", p.getContent());
            map.put("userId", p.getUserId());
            map.put("userNickname", nicknameMap.getOrDefault(p.getUserId(), ""));
            map.put("likeCount", p.getLikeCount());
            map.put("commentCount", p.getCommentCount());
            map.put("status", p.getStatus());
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    @PutMapping("/posts/{id}/status")
    public Result<Void> updatePostStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(ResultCode.PARAM_ERROR, "动态不存在");
        Integer status = body.get("status");
        if (status == null) throw new BusinessException(ResultCode.PARAM_ERROR, "status 必填");
        post.setStatus(status);
        postMapper.updateById(post);
        return Result.ok();
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(ResultCode.PARAM_ERROR, "动态不存在");
        postMapper.deleteById(id);
        return Result.ok();
    }
}
