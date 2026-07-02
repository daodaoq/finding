package com.finding.controller;

import com.finding.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 图片代理控制器 —— 从 MinIO 读取图片并返回给浏览器。
 * 浏览器不直接访问 MinIO，避免 403 Forbidden 问题。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @GetMapping("/api/v1/images/{objectName}")
    public void getImage(@PathVariable String objectName, HttpServletResponse response) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucket())
                .object(objectName)
                .build())) {

            // 根据扩展名设置 Content-Type
            String contentType = getContentType(objectName);
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=86400");

            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = stream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.debug("读取图片失败: {}", objectName);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException ignored) {}
        }
    }

    private String getContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg"; // 默认
    }
}
