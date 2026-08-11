package com.finding.app.controller;

import com.finding.framework.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
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

        } catch (ErrorResponseException e) {
            // 文件不存在是预期情况(浏览器可能请求已删除的图片) → 404;其余 MinIO 错误需告警
            if (e.errorResponse() != null && "NoSuchKey".equals(e.errorResponse().code())) {
                log.debug("图片不存在: {}", objectName);
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
            } else {
                log.error("读取图片失败(MinIO 错误): object={}, code={}", objectName,
                        e.errorResponse() != null ? e.errorResponse().code() : "unknown", e);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            // 配置/签名等基础设施错误:告警并返回 500,避免被 404 掩盖
            log.error("读取图片失败(MinIO 基础设施): object={}", objectName, e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            // 浏览器中途断开/取消下载属预期可忽略
            log.debug("图片流输出中断: object={}, cause={}", objectName, e.getMessage());
        }
    }

    /** 写入错误状态;响应已提交或连接已断开时无法再写,可忽略 */
    private void sendError(HttpServletResponse response, int status) {
        try {
            response.sendError(status);
        } catch (IOException ignored) {
            // 连接已断开/响应已提交,无法再写错误状态
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
