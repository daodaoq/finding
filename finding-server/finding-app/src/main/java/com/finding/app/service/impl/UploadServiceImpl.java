package com.finding.app.service.impl;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.framework.config.MinioConfig;
import com.finding.app.service.UploadService;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO 对象存储实现 —— 图片上传到 MinIO，返回访问 URL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${finding.upload.max-size:5242880}")
    private long maxSize;

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    /** 初始化：确保 Bucket 存在 */
    @PostConstruct
    public void init() {
        try {
            String bucket = minioConfig.getBucket();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO Bucket 已创建: {}", bucket);
            }
            log.info("MinIO 连接成功, endpoint={}, bucket={}", minioConfig.getEndpoint(), bucket);
        } catch (Exception e) {
            log.error("MinIO 初始化失败", e);
        }
    }

    @Override
    public String uploadImage(byte[] data, String originalFilename, String contentType) {
        // 校验大小
        if (data.length > maxSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件大小超过限制(5MB)");
        }
        // 校验类型(Content-Type)
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型，仅允许 JPEG/PNG/WebP");
        }
        // 校验魔数(防伪造 Content-Type 上传任意文件)
        validateMagic(data);

        String ext = getExtension(originalFilename);
        String objectName = UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());

            // 返回后端代理 URL（浏览器不直接访问 MinIO，避免 403）
            String url = "/api/v1/images/" + objectName;
            log.info("图片已上传至 MinIO: {} -> {}", originalFilename, url);
            return url;
        } catch (Exception e) {
            log.error("上传 MinIO 失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败");
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot).toLowerCase() : ".jpg";
    }

    /** 校验文件魔数:JPEG(FF D8 FF) / PNG(89 50 4E 47) / WebP(RIFF....WEBP) */
    private void validateMagic(byte[] data) {
        if (data.length < 12) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型，仅允许 JPEG/PNG/WebP");
        }
        boolean jpeg = (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
        boolean png = (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47;
        boolean webp = data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
        if (!jpeg && !png && !webp) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型，仅允许 JPEG/PNG/WebP");
        }
    }
}
