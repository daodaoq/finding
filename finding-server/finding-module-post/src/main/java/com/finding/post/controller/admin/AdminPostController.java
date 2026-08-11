package com.finding.post.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.post.dto.PostCreateDTO;
import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import com.finding.common.audit.OperationAuditService;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtInterceptor;
import com.finding.message.service.MessageService;
import com.finding.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    private final MessageService messageService;
    private final OperationAuditService operationAuditService;

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
            map.put("reviewStatus", p.getReviewStatus() != null ? p.getReviewStatus() : 0);
            map.put("reviewReason", p.getReviewReason());
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 编辑动态(管理端,无所有权限制) */
    @PutMapping("/posts/{id}")
    public Result<Void> updatePost(@PathVariable Long id, @RequestBody PostCreateDTO dto) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(ResultCode.PARAM_ERROR, "动态不存在");
        post.setContent(dto.getContent());
        post.setImages(dto.getImages() != null ? String.join(",", dto.getImages()) : null);
        post.setLocation(dto.getLocation());
        post.setCity(dto.getCity());
        if (dto.getLatitude() != null) post.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) post.setLongitude(dto.getLongitude());
        postMapper.updateById(post);
        return Result.ok();
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

    /** 待审核队列(review_status=1) */
    @GetMapping("/posts/review")
    public Result<PageVO<Map<String, Object>>> reviewQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Post> result = postMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .eq(Post::getReviewStatus, 1)
                        .orderByAsc(Post::getCreatedAt));

        Set<Long> userIds = result.getRecords().stream().map(Post::getUserId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("content", p.getContent());
            map.put("images", p.getImages() != null ? List.of(p.getImages().split(",")) : List.of());
            map.put("userId", p.getUserId());
            map.put("userNickname", nicknameMap.getOrDefault(p.getUserId(), "用户" + p.getUserId()));
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).toList();
        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 审核处理:pass=true 通过(发布),false 拒绝(需 reason);记录审核人/时间/原因,拒绝时通知作者 */
    @PutMapping("/posts/{id}/review")
    public Result<Void> reviewPost(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(ResultCode.PARAM_ERROR, "动态不存在");
        Boolean pass = body.get("pass") != null ? Boolean.parseBoolean(body.get("pass").toString()) : true;
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;

        post.setReviewStatus(pass ? 0 : 2);
        post.setReviewReason(pass ? null : reason);
        post.setReviewBy(JwtInterceptor.getCurrentUserId());
        post.setReviewTime(LocalDateTime.now());
        postMapper.updateById(post);

        // 拒绝时通知作者(可看到原因)
        if (!pass && post.getUserId() != null) {
            String content = reason != null && !reason.isBlank() ? "你的动态审核未通过：" + reason : "你的动态审核未通过";
            messageService.notify(JwtInterceptor.getCurrentUserId(), post.getUserId(), "post_rejected", content, post.getId());
        }
        operationAuditService.record(JwtInterceptor.getCurrentUserId(), "post_review", "post", post.getId(),
                pass ? "审核通过" : "审核拒绝", reason);
        return Result.ok();
    }
}
