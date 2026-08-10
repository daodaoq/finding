package com.finding.post.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.PageVO;
import com.finding.post.entity.Post;
import com.finding.post.entity.PostComment;
import com.finding.post.entity.PostCommentLike;
import com.finding.post.mapper.PostCommentLikeMapper;
import com.finding.post.mapper.PostCommentMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 评论管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCommentController {

    private final PostCommentMapper commentMapper;
    private final PostCommentLikeMapper commentLikeMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    @GetMapping("/comments")
    public Result<PageVO<Map<String, Object>>> listComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<PostComment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PostComment::getContent, keyword);
        }
        wrapper.orderByDesc(PostComment::getCreatedAt);

        Page<PostComment> result = commentMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量取作者昵称 + 所属动态内容
        Set<Long> userIds = new HashSet<>();
        Set<Long> postIds = new HashSet<>();
        result.getRecords().forEach(c -> {
            userIds.add(c.getUserId());
            postIds.add(c.getPostId());
        });
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }
        Map<Long, String> postContentMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            postMapper.selectBatchIds(postIds).forEach(p -> postContentMap.put(p.getId(), p.getContent()));
        }

        List<Map<String, Object>> records = result.getRecords().stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("postId", c.getPostId());
            map.put("postContent", postContentMap.getOrDefault(c.getPostId(), ""));
            map.put("authorNickname", nicknameMap.getOrDefault(c.getUserId(), "用户" + c.getUserId()));
            map.put("content", c.getContent());
            map.put("likeCount", c.getLikeCount());
            map.put("parentId", c.getParentId());
            map.put("createdAt", c.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 编辑评论内容(管理员可改一切) */
    @PutMapping("/comments/{id}")
    public Result<Void> updateComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        PostComment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        String content = body.get("content");
        if (!StringUtils.hasText(content)) throw new BusinessException(ResultCode.PARAM_ERROR, "内容不能为空");
        comment.setContent(content);
        commentMapper.updateById(comment);
        return Result.ok();
    }

    /** 软删除评论(保留审计,前端显示占位) */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        PostComment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        comment.setStatus(1);
        commentMapper.updateById(comment);
        return Result.ok();
    }
}
