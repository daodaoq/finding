package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminPostService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.post.dto.PostCreateDTO;
import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPostServiceImpl implements AdminPostService {

    /** 内容状态:0=已删除 1=发布中 2=已下架 */
    private static final Set<Integer> ALLOWED_STATUS = Set.of(0, 1, 2);
    /** 单次批量审核上限,避免一次请求处理过多内容 */
    private static final int MAX_BATCH = 100;

    private final PostMapper postMapper;
    private final AdminUserLookupService userLookupService;
    private final MessageService messageService;
    private final OperationAuditService operationAuditService;

    @Override
    public PageVO<Map<String, Object>> listPosts(int page, int size, String keyword) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Post::getContent, keyword).or().eq(Post::getId, keyword));
        }
        wrapper.orderByDesc(Post::getCreatedAt);

        Page<Post> result = postMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(
                result.getRecords().stream().map(Post::getUserId).collect(Collectors.toSet()));

        List<Map<String, Object>> records = result.getRecords().stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("content", p.getContent());
            map.put("userId", p.getUserId());
            map.put("userNickname", nicknameMap.getOrDefault(p.getUserId(), ""));
            map.put("likeCount", p.getLikeCount());
            map.put("commentCount", p.getCommentCount());
            map.put("isTop", p.getIsTop());
            map.put("isHot", p.getIsHot());
            map.put("status", p.getStatus());
            map.put("reviewStatus", p.getReviewStatus() != null ? p.getReviewStatus() : 0);
            map.put("reviewReason", p.getReviewReason());
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).toList();

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updatePost(Long id, PostCreateDTO dto) {
        Post post = requirePost(id);
        post.setContent(dto.getContent());
        post.setImages(dto.getImages() != null ? String.join(",", dto.getImages()) : null);
        post.setLocation(dto.getLocation());
        post.setCity(dto.getCity());
        if (dto.getLatitude() != null) post.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) post.setLongitude(dto.getLongitude());
        postMapper.updateById(post);
    }

    @Override
    public void updatePostStatus(Long adminId, Long id, Map<String, Object> body) {
        Post post = requirePost(id);
        Integer status = asInt(body.get("status"));
        if (status == null) throw new BusinessException(ResultCode.PARAM_ERROR, "status 必填");
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 仅支持 0=删除 1=发布 2=下架");
        }
        post.setStatus(status);
        postMapper.updateById(post);
        // 下架/删除通知作者(不通知恢复)
        if (post.getUserId() != null && status != 1) {
            String text = status == 0 ? "你的动态已被管理员删除" : "你的动态已被管理员下架";
            messageService.notify(adminId, post.getUserId(), "post_admin_action", text, id);
        }
    }

    @Override
    public void updatePostFlag(Long adminId, Long id, Map<String, Object> body) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getStatus() == 0) throw new BusinessException(ResultCode.PARAM_ERROR, "动态不存在");
        Integer isTop = asInt(body.get("isTop"));
        Integer isHot = asInt(body.get("isHot"));
        if (isTop != null) post.setIsTop(isTop == 1 ? 1 : 0);
        if (isHot != null) post.setIsHot(isHot == 1 ? 1 : 0);
        postMapper.updateById(post);
        operationAuditService.record(adminId, "post_flag", "post", id,
                "设置置顶/精华", "isTop=" + post.getIsTop() + ", isHot=" + post.getIsHot());
    }

    @Override
    public void deletePost(Long adminId, Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getStatus() == 0) throw new BusinessException(ResultCode.PARAM_ERROR, "动态不存在");
        // 软删(与用户端一致):保留记录与审计,前端/查询按 status!=1 过滤
        post.setStatus(0);
        postMapper.updateById(post);
        operationAuditService.record(adminId, "post_delete", "post", id, "删除动态", null);
        if (post.getUserId() != null) {
            messageService.notify(adminId, post.getUserId(), "post_admin_action", "你的动态已被管理员删除", id);
        }
    }

    @Override
    public PageVO<Map<String, Object>> reviewQueue(int page, int size) {
        size = AdminPaging.clampSize(size);
        Page<Post> result = postMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .eq(Post::getReviewStatus, 1)
                        .orderByAsc(Post::getCreatedAt));

        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(result.getRecords().stream()
                .map(Post::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));

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
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void reviewPost(Long adminId, Long id, Map<String, Object> body) {
        Post post = requirePost(id);
        // pass 缺省视为不通过:请求体缺字段时绝不放行内容
        boolean pass = asBoolean(body.get("pass"), false);
        String reason = asString(body.get("reason"));
        applyReview(adminId, post, pass, reason);
        operationAuditService.record(adminId, "post_review", "post", post.getId(),
                pass ? "审核通过" : "审核拒绝", reason);
    }

    @Override
    @Transactional
    public void reviewBatch(Long adminId, Map<String, Object> body) {
        List<Long> ids = parseIds(body.get("ids"));
        if (ids.isEmpty()) throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要处理的动态");
        if (ids.size() > MAX_BATCH) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "单次最多处理 " + MAX_BATCH + " 条");
        }
        boolean pass = asBoolean(body.get("pass"), false);
        String reason = asString(body.get("reason"));

        int count = 0;
        for (Long id : ids) {
            Post post = postMapper.selectById(id);
            if (post == null) continue;
            applyReview(adminId, post, pass, reason);
            count++;
        }
        operationAuditService.record(adminId, "post_review_batch", "post", null,
                pass ? "批量审核通过" : "批量审核拒绝", "共 " + count + " 条");
    }

    /** 审核落地:单条与批量共用同一实现,避免两处复制 */
    private void applyReview(Long adminId, Post post, boolean pass, String reason) {
        post.setReviewStatus(pass ? 0 : 2);
        post.setReviewReason(pass ? null : reason);
        post.setReviewBy(adminId);
        post.setReviewTime(LocalDateTime.now());
        postMapper.updateById(post);
        if (!pass && post.getUserId() != null) {
            String content = StringUtils.hasText(reason) ? "你的动态审核未通过：" + reason : "你的动态审核未通过";
            messageService.notify(adminId, post.getUserId(), "post_rejected", content, post.getId());
        }
    }

    private Post requirePost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(ResultCode.PARAM_ERROR, "动态不存在");
        return post;
    }

    /** 整批解析 ids:任一非法值直接报参数错误,不再中途抛 NumberFormatException 造成半途落库 */
    private List<Long> parseIds(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return ids;
        for (Object item : list) {
            if (item == null) continue;
            try {
                ids.add(Long.valueOf(String.valueOf(item).trim()));
            } catch (NumberFormatException e) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "动态 id 不合法:" + item);
            }
        }
        return ids;
    }

    private Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "参数格式不正确:" + value);
        }
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
