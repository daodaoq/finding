package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminImageModerationService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.moderation.ImageModeration;
import com.finding.common.moderation.ImageModerationMapper;
import com.finding.framework.config.MinioConfig;
import com.finding.message.service.MessageService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminImageModerationServiceImpl implements AdminImageModerationService {

    private final ImageModerationMapper imageModerationMapper;
    private final AdminUserLookupService userLookupService;
    private final MessageService messageService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public PageVO<Map<String, Object>> reviewQueue(int page, int size) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<ImageModeration> wrapper = new LambdaQueryWrapper<ImageModeration>()
                .eq(ImageModeration::getVerdict, 2)
                .eq(ImageModeration::getStatus, 0)
                .orderByDesc(ImageModeration::getCreatedAt);
        Page<ImageModeration> result = imageModerationMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, String> nicknameMap = userLookupService.nicknamesByIds(result.getRecords().stream()
                .map(ImageModeration::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));

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
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void handle(Long adminId, Long id, Map<String, Object> body) {
        ImageModeration m = imageModerationMapper.selectById(id);
        if (m == null) throw new BusinessException(ResultCode.PARAM_ERROR, "审核记录不存在");
        if (m.getStatus() != null && m.getStatus() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该记录已处理");
        }
        boolean pass = body.get("pass") != null && Boolean.parseBoolean(body.get("pass").toString());
        String note = body.get("note") != null ? body.get("note").toString() : null;

        m.setStatus(pass ? 1 : 2);
        m.setReviewBy(adminId);
        m.setReviewNote(note);
        m.setReviewTime(LocalDateTime.now());
        imageModerationMapper.updateById(m);

        // 驳回 → 删除已上传对象
        if (!pass && m.getImageUrl() != null) {
            String objectName = minioObjectName(m.getImageUrl());
            if (objectName != null) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket()).object(objectName).build());
                } catch (Exception e) {
                    // 删除失败不阻断复核结果落库,记日志即可
                    log.warn("图片复核驳回后删除 MinIO 对象失败,object={}", objectName, e);
                }
            }
        }
        if (m.getUserId() != null) {
            messageService.notify(adminId, m.getUserId(),
                    pass ? "image_audit_approved" : "image_audit_rejected",
                    pass ? "你上传的图片已通过复核" : ("你上传的图片因违规被移除"
                            + (note != null && !note.isBlank() ? "：" + note : "")),
                    m.getId());
        }
    }

    /**
     * 从图片 URL 推导 MinIO 对象名:先剥掉 {@code ?query}(含预签名参数),
     * 再取最后一段路径;空白输入或推不出对象名时返回 null。
     */
    private String minioObjectName(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) return null;
        String path = imageUrl.trim();
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) path = path.substring(0, queryIndex);
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return StringUtils.hasText(name) ? name : null;
    }
}
