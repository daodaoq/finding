package com.finding.post.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.post.entity.Appeal;
import com.finding.post.entity.Post;
import com.finding.post.mapper.AppealMapper;
import com.finding.post.mapper.PostMapper;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 内容申诉 —— 用户对审核未通过的动态提起申诉 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AppealController {

    private final AppealMapper appealMapper;
    private final PostMapper postMapper;

    /** 对审核未通过的动态发起申诉(同一动态不可重复待处理申诉) */
    @PostMapping("/posts/{id}/appeal")
    public Result<Void> appeal(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        Post post = postMapper.selectById(id);
        if (post == null || post.getStatus() == 0) throw new BusinessException(ResultCode.POST_NOT_FOUND);
        if (!post.getUserId().equals(userId)) throw new BusinessException(ResultCode.PARAM_ERROR, "只能申诉自己的动态");
        if (post.getReviewStatus() == null || post.getReviewStatus() != 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅审核未通过的动态可申诉");
        }
        Long pending = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getTargetType, "post")
                .eq(Appeal::getTargetId, id)
                .eq(Appeal::getStatus, 0));
        if (pending > 0) throw new BusinessException(ResultCode.PARAM_ERROR, "已有待处理的申诉，请耐心等待");
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        if (!StringUtils.hasText(reason)) throw new BusinessException(ResultCode.PARAM_ERROR, "请填写申诉理由");

        Appeal a = new Appeal();
        a.setUserId(userId);
        a.setTargetType("post");
        a.setTargetId(id);
        a.setReason(reason);
        a.setStatus(0);
        a.setOriginalResult(post.getReviewReason());
        appealMapper.insert(a);
        return Result.ok();
    }

    /** 我的申诉记录 */
    @GetMapping("/appeals/mine")
    public Result<List<Map<String, Object>>> myAppeals() {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        List<Appeal> list = appealMapper.selectList(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getUserId, userId)
                .orderByDesc(Appeal::getCreatedAt));
        return Result.ok(list.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("targetType", a.getTargetType());
            m.put("targetId", a.getTargetId());
            m.put("reason", a.getReason());
            m.put("status", a.getStatus());
            m.put("handleNote", a.getHandleNote());
            m.put("handleTime", a.getHandleTime());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).toList());
    }
}
