package com.finding.app.controller;

import com.finding.framework.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
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
 * 文件代理控制器 —— 从 MinIO 读取对象并返回给浏览器。
 * 浏览器不直接访问 MinIO，避免 403 Forbidden 问题。
 * 支持 HTTP Range(206 部分内容),视频可正常拖动进度条/seek;图片不受影响。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @GetMapping("/api/v1/images/{objectName}")
    public void getFile(@PathVariable String objectName, HttpServletRequest request, HttpServletResponse response) {
        String range = request.getHeader("Range");
        boolean hasRange = range != null && range.startsWith("bytes=");
        long total = -1;
        long start = 0;
        long end = -1;
        try {
            if (hasRange) {
                StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(minioConfig.getBucket()).object(objectName).build());
                total = stat.size();
                long[] se = parseRange(range, total);
                start = se[0];
                end = se[1];
                if (start > end || start >= total) {
                    // 区间越界 → 416,并告知资源总大小
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    response.setHeader("Content-Range", "bytes */" + total);
                    return;
                }
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + total);
                response.setHeader("Content-Length", String.valueOf(end - start + 1));
            } else {
                response.setHeader("Accept-Ranges", "bytes");
            }
            response.setContentType(getContentType(objectName));
            response.setHeader("Cache-Control", "public, max-age=86400");

            GetObjectArgs.Builder args = GetObjectArgs.builder()
                    .bucket(minioConfig.getBucket()).object(objectName);
            if (hasRange) {
                args.offset(start).length(end - start + 1);
            }
            try (InputStream stream = minioClient.getObject(args.build())) {
                OutputStream out = response.getOutputStream();
                byte[] buf = new byte[8192];
                int len;
                // 兜底只写出请求范围内的字节(MinIO 按 offset/length 已裁剪,此处防意外)
                long remaining = hasRange ? end - start + 1 : Long.MAX_VALUE;
                while (remaining > 0 && (len = stream.read(buf)) != -1) {
                    int toWrite = (int) Math.min(len, remaining);
                    out.write(buf, 0, toWrite);
                    remaining -= toWrite;
                }
                out.flush();
            }

        } catch (ErrorResponseException e) {
            // 文件不存在是预期情况(浏览器可能请求已删除的图片) → 404;其余 MinIO 错误需告警
            if (e.errorResponse() != null && "NoSuchKey".equals(e.errorResponse().code())) {
                log.debug("文件不存在: {}", objectName);
                sendError(response, HttpServletResponse.SC_NOT_FOUND);
            } else {
                log.error("读取文件失败(MinIO 错误): object={}, code={}", objectName,
                        e.errorResponse() != null ? e.errorResponse().code() : "unknown", e);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            // 配置/签名等基础设施错误:告警并返回 500,避免被 404 掩盖
            log.error("读取文件失败(MinIO 基础设施): object={}", objectName, e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            // 浏览器中途断开/取消下载属预期可忽略
            log.debug("文件流输出中断: object={}, cause={}", objectName, e.getMessage());
        }
    }

    /** 解析 Range 为 [start, end]:
     *  bytes=start-end / bytes=start-(开区间) / bytes=-N(末尾 N 字节);非法与越界做边界兜底 */
    private long[] parseRange(String range, long total) {
        String spec = range.substring("bytes=".length()).trim();
        String[] parts = spec.split("-", 2);
        long start;
        long end;
        if (parts[0].trim().isEmpty()) {
            // 后缀区间: bytes=-N → 最后 N 字节
            long n;
            try {
                n = Long.parseLong(parts[1].trim());
            } catch (NumberFormatException e) {
                n = 0;
            }
            start = Math.max(0, total - n);
            end = total - 1;
        } else {
            try {
                start = Math.max(0, Math.min(Long.parseLong(parts[0].trim()), total - 1));
            } catch (NumberFormatException e) {
                start = 0;
            }
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                try {
                    end = Math.min(Long.parseLong(parts[1].trim()), total - 1);
                } catch (NumberFormatException ignored) {
                    end = total - 1;
                }
            } else {
                end = total - 1;
            }
        }
        return new long[]{start, end};
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
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mov")) return "video/quicktime";
        return "image/jpeg"; // 默认
    }
}
