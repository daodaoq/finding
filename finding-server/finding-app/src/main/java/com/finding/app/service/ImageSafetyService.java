package com.finding.app.service;

/**
 * 图片内容安全 —— 上传前鉴黄(阿里云内容安全 ImageModeration)+ OCR 提取文字过违禁词。
 * <p>判定为三态:{@link ModerationVerdict#BLOCK} 拦截 / {@link ModerationVerdict#REVIEW} 送审 /
 * {@link ModerationVerdict#PASS} 放行。调用失败或未开启时返回 PASS,不阻断上传。
 * 密钥/开关由配置项控制,未开启或未配置 key 时直接跳过。</p>
 */
public interface ImageSafetyService {

    /** 校验图片:鉴黄 + OCR 提取文字过违禁词,返回三态判定(不在此层抛异常) */
    ImageModerationResult check(byte[] data, String imageUrl);
}
