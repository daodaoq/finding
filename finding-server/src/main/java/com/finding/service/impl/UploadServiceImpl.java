package com.finding.service.impl;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.service.UploadService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    @Value("${finding.upload.path:./uploads}")
    private String uploadPath;

    @Value("${finding.upload.max-size:5242880}")
    private long maxSize;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
    }

    @Override
    public String uploadImage(byte[] data, String originalFilename, String contentType) {
        // Validate size
        if (data.length > maxSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件大小超过限制(5MB)");
        }
        // Validate type
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型，仅允许JPEG/PNG/WebP");
        }

        String ext = getExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetPath = basePath.resolve(newFilename);

        try {
            Files.write(targetPath, data);
            log.info("File uploaded: {} -> {}", originalFilename, targetPath);
            return "/uploads/" + newFilename;
        } catch (IOException e) {
            log.error("Failed to save uploaded file", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败");
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            return filename.substring(dot).toLowerCase();
        }
        return ".jpg";
    }
}
