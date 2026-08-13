package com.finding.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.post.entity.Appeal;
import com.finding.post.entity.Post;
import com.finding.post.mapper.AppealMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.post.service.AppealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 申诉闭环:被拒(reviewStatus=2)或被下架(status=2)的动态可申诉,
 * 同一内容最多 {@link #MAX_APPEALS_PER_TARGET} 次,避免无限申诉刷队列。
 */
@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    /** 内容审核状态:0=已发布 1=待审 2=拒绝 */
    private static final int POST_REVIEW_REJECTED = 2;
    /** 帖子状态:0=删除 1=正常 2=下架 */
    private static final int POST_STATUS_DELETED = 0;
    private static final int POST_STATUS_HIDDEN = 2;
    /** 申诉状态:0=待处理 */
    private static final int APPEAL_STATUS_PENDING = 0;
    /** 同一内容申诉次数上限 */
    private static final int MAX_APPEALS_PER_TARGET = 3;

    private final AppealMapper appealMapper;
    private final PostMapper postMapper;

    @Override
    public void appeal(Long userId, Long postId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请填写申诉理由");
        }
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == POST_STATUS_DELETED) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能申诉自己的动态");
        }
        // 仅被拒或下架内容可申诉(去掉硬编码:被拒=reviewStatus 2,下架=status 2)
        boolean rejected = post.getReviewStatus() != null && post.getReviewStatus() == POST_REVIEW_REJECTED;
        boolean hidden = post.getStatus() == POST_STATUS_HIDDEN;
        if (!rejected && !hidden) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅审核未通过或已下架的动态可申诉");
        }

        Long pending = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getTargetType, "post")
                .eq(Appeal::getTargetId, postId)
                .eq(Appeal::getStatus, APPEAL_STATUS_PENDING));
        if (pending > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已有待处理的申诉，请耐心等待");
        }
        Long total = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getTargetType, "post")
                .eq(Appeal::getTargetId, postId));
        if (total >= MAX_APPEALS_PER_TARGET) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该内容的申诉次数已达上限");
        }

        Appeal a = new Appeal();
        a.setUserId(userId);
        a.setTargetType("post");
        a.setTargetId(postId);
        a.setReason(reason);
        a.setStatus(APPEAL_STATUS_PENDING);
        a.setOriginalResult(StringUtils.hasText(post.getReviewReason()) ? post.getReviewReason() : "内容已下架");
        appealMapper.insert(a);
    }

    @Override
    public List<Map<String, Object>> myAppeals(Long userId) {
        List<Appeal> list = appealMapper.selectList(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getUserId, userId)
                .orderByDesc(Appeal::getCreatedAt));
        return list.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("targetType", a.getTargetType());
            m.put("targetId", a.getTargetId());
            m.put("reason", a.getReason());
            m.put("status", a.getStatus());
            m.put("originalResult", a.getOriginalResult());
            m.put("handleNote", a.getHandleNote());
            m.put("handleTime", a.getHandleTime());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).toList();
    }
}
