package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminCommentService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.post.entity.Post;
import com.finding.post.entity.PostComment;
import com.finding.post.mapper.PostCommentMapper;
import com.finding.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminCommentServiceImpl implements AdminCommentService {

    /** 评论状态:1=已删除 */
    private static final int STATUS_DELETED = 1;

    private final PostCommentMapper commentMapper;
    private final PostMapper postMapper;
    private final AdminUserLookupService userLookupService;
    private final OperationAuditService operationAuditService;

    @Override
    public PageVO<Map<String, Object>> listComments(int page, int size, String keyword) {
        size = AdminPaging.clampSize(size);
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
        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(userIds);
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

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updateComment(Long id, Map<String, String> body) {
        PostComment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        String content = body.get("content");
        if (!StringUtils.hasText(content)) throw new BusinessException(ResultCode.PARAM_ERROR, "内容不能为空");
        comment.setContent(content);
        commentMapper.updateById(comment);
    }

    @Override
    public void deleteComment(Long adminId, Long id) {
        PostComment comment = commentMapper.selectById(id);
        if (comment == null) throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        comment.setStatus(STATUS_DELETED);
        commentMapper.updateById(comment);
        operationAuditService.record(adminId, "comment_delete", "comment", id, "删除评论", null);
    }
}
