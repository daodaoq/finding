package com.finding.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.moderation.ImageModeration;
import com.finding.common.moderation.ImageModerationMapper;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.framework.config.MinioConfig;
import com.finding.message.service.MessageService;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtInterceptor;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员 - 图片审核复核队列。机器送审(review)的图片进入此队列,人工放行或删除。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminImageModerationController {

    private final ImageModerationMapper imageModerationMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /** 待复核队列(verdict=2 送审 且 status=0 待复核) */
    @GetMapping("/image-moderation")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<ImageModeration> wrapper = new LambdaQueryWrapper<ImageModeration>()
                .eq(ImageModeration::getVerdict, 2)
                .eq(ImageModeration::getStatus, 0)
                .orderByDesc(ImageModeration::getCreatedAt);
        Page<ImageModeration> result = imageModerationMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> userIds = result.getRecords().stream().map(ImageModeration::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }
        List<Map<String, Object>> records = result.getRecords().stream().map(m -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", m.getId());
            e.put("userId", m.getUserId());
            e.put("userNickname", nicknameMap.getOrDefault(m.getUserId(), "用户" + m.getUserId()));
            e.put("imageUrl", m.getImageUrl());
            e.put("scene", m.getScene());
            e.put("riskLevel", m.getRiskLevel());
            e.put("ocrText", m.getOcrText());
            e.put("createdAt", m.getCreatedAt());
            return e;
        }).toList();
        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 复核:pass=true 放行,false 删除图片并通知上传者 */
    @PutMapping("/image-moderation/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ImageModeration m = imageModerationMapper.selectById(id);
        if (m == null) throw new BusinessException(ResultCode.PARAM_ERROR, "审核记录不存在");
        if (m.getStatus() != null && m.getStatus() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该记录已处理");
        }
        boolean pass = body.get("pass") != null && Boolean.parseBoolean(body.get("pass").toString());
        String note = body.get("note") != null ? body.get("note").toString() : null;
        Long adminId = JwtInterceptor.getCurrentUserId();

        m.setStatus(pass ? 1 : 2);
        m.setReviewBy(adminId);
        m.setReviewNote(note);
        m.setReviewTime(LocalDateTime.now());
        imageModerationMapper.updateById(m);

        // 驳回 → 删除已上传对象
        if (!pass && m.getImageUrl() != null) {
            try {
                String objectName = m.getImageUrl().substring(m.getImageUrl().lastIndexOf('/') + 1);
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioConfig.getBucket()).object(objectName).build());
            } catch (Exception e) {
                // 删除失败不阻断复核结果落库,记日志即可
            }
        }
        if (m.getUserId() != null) {
            messageService.notify(adminId, m.getUserId(),
                    pass ? "image_audit_approved" : "image_audit_rejected",
                    pass ? "你上传的图片已通过复核" : ("你上传的图片因违规被移除"
                            + (note != null && !note.isBlank() ? "：" + note : "")),
                    m.getId());
        }
        return Result.ok();
    }
}
