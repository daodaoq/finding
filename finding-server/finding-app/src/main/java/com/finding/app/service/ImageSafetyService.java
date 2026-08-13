package com.finding.app.service;

import com.aliyun.green20220302.models.ImageModerationRequest;
import com.aliyun.green20220302.models.ImageModerationResponse;
import com.aliyun.ocr_api20210707.models.RecognizeAdvancedRequest;
import com.aliyun.ocr_api20210707.models.RecognizeAdvancedResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finding.common.word.ReviewResult;
import com.finding.common.word.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

/**
 * 图片内容安全 —— 上传前鉴黄(阿里云内容安全 ImageModeration)+ OCR 提取文字过违禁词。
 * <p>判定为三态:{@link ModerationVerdict#BLOCK} 拦截 / {@link ModerationVerdict#REVIEW} 送审 /
 * {@link ModerationVerdict#PASS} 放行。调用失败或未开启时返回 PASS,不阻断上传。
 * 密钥/开关由配置项控制,未开启或未配置 key 时直接跳过。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSafetyService {

    /** 命中断言违规的 riskLevel 集合 → 拦截 */
    private static final Set<String> REJECT_LEVELS = Set.of("block", "high");
    /** 中等风险 riskLevel 集合 → 送审(不拦截,进后台复核队列) */
    private static final Set<String> REVIEW_LEVELS = Set.of("review", "medium");

    private final SensitiveWordFilter sensitiveWordFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${finding.image-safety.enabled:false}")
    private boolean enabled;

    @Value("${finding.image-safety.access-key-id:}")
    private String accessKeyId;

    @Value("${finding.image-safety.access-key-secret:}")
    private String accessKeySecret;

    @Value("${finding.image-safety.moderation-region:cn-shanghai}")
    private String moderationRegion;

    @Value("${finding.image-safety.ocr-region:cn-hangzhou}")
    private String ocrRegion;

    /** 校验图片:鉴黄 + OCR 提取文字过违禁词,返回三态判定(不在此层抛异常) */
    public ImageModerationResult check(byte[] data, String imageUrl) {
        String riskLevel = enabled ? moderate(imageUrl) : "pass";
        String ocrText = enabled ? recognizeText(data) : null;
        return classify(riskLevel, ocrText);
    }

    /** 纯判定逻辑(独立出便于单测):风险等级 + OCR 文字 → 三态 */
    ImageModerationResult classify(String riskLevel, String ocrText) {
        String level = riskLevel == null ? "" : riskLevel.toLowerCase();
        if (REJECT_LEVELS.contains(level)) {
            return new ImageModerationResult(level, ocrText, ModerationVerdict.BLOCK);
        }
        // OCR 文字过违禁词:拦截词优先于送审词,二者都优先于风险等级
        if (StringUtils.hasText(ocrText)) {
            ReviewResult rr = sensitiveWordFilter.classifyReview(ocrText);
            if (rr.hasBlocking()) {
                return new ImageModerationResult(level, ocrText, ModerationVerdict.BLOCK);
            }
            if (rr.hasReview()) {
                return new ImageModerationResult(level, ocrText, ModerationVerdict.REVIEW);
            }
        }
        if (REVIEW_LEVELS.contains(level)) {
            return new ImageModerationResult(level, ocrText, ModerationVerdict.REVIEW);
        }
        return new ImageModerationResult(level, ocrText, ModerationVerdict.PASS);
    }

    /**
     * 鉴黄:返回 pass / review / block。
     * 未配置公网地址(本地开发)或调用失败时放行返回 pass,不阻断上传。
     */
    private String moderate(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return "pass";
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint("green-cip." + moderationRegion + ".aliyuncs.com");
            com.aliyun.green20220302.Client client = new com.aliyun.green20220302.Client(config);
            ImageModerationRequest req = new ImageModerationRequest()
                    .setService("baselineCheck")
                    .setServiceParameters("{\"imageUrl\": \"" + imageUrl + "\", \"dataId\": \""
                            + java.util.UUID.randomUUID() + "\"}");
            ImageModerationResponse resp = client.imageModeration(req);
            var body = resp.getBody();
            if (body == null || body.getData() == null) {
                return "pass";
            }
            List<com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyDataResult> results =
                    body.getData().getResult();
            if (results != null && !results.isEmpty()) {
                String riskLevel = results.get(0).getRiskLevel();
                return StringUtils.hasText(riskLevel) ? riskLevel : "pass";
            }
            return "pass";
        } catch (Exception e) {
            log.warn("图片鉴黄调用失败，放行: {}", e.getMessage());
            return "pass";
        }
    }

    /** OCR 提取图片文字(通用文字识别),失败返回 null */
    private String recognizeText(byte[] data) {
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint("ocr-api." + ocrRegion + ".aliyuncs.com");
            com.aliyun.ocr_api20210707.Client client = new com.aliyun.ocr_api20210707.Client(config);
            RecognizeAdvancedRequest req = new RecognizeAdvancedRequest();
            req.setBody(new ByteArrayInputStream(data));
            RecognizeAdvancedResponse resp = client.recognizeAdvanced(req);
            var body = resp.getBody();
            if (body != null && StringUtils.hasText(body.getData())) {
                // data 是 JSON 字符串,提取 content 字段(识别出的全文)
                JsonNode node = objectMapper.readTree(body.getData());
                return node.path("content").asText("");
            }
            return null;
        } catch (Exception e) {
            log.warn("OCR 调用失败，跳过文字校验: {}", e.getMessage());
            return null;
        }
    }
}
