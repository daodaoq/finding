package com.finding.app.service;

import com.aliyun.green20220302.models.ImageModerationRequest;
import com.aliyun.green20220302.models.ImageModerationResponse;
import com.aliyun.ocr_api20210707.models.RecognizeAdvancedRequest;
import com.aliyun.ocr_api20210707.models.RecognizeAdvancedResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

/**
 * 图片内容安全 —— 上传前鉴黄(阿里云内容安全 ImageModeration)+ OCR 提取文字过违禁词。
 * <p>调用失败时放行(不阻断上传),仅在明确返回 block 时拒绝,保证第三方服务异常不影响业务。
 * 密钥/开关由配置项控制,未开启或未配置 key 时直接跳过。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSafetyService {

    /** 命中断言违规的 riskLevel 集合(其余 none/low/medium/review 视为通过) */
    private static final java.util.Set<String> REJECT_LEVELS = java.util.Set.of("block", "high");

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

    /** 校验图片:鉴黄(imageUrl 公网可访问时生效)+ OCR 提取文字过违禁词 */
    public void check(byte[] data, String imageUrl) {
        if (!enabled) {
            return;
        }
        if (REJECT_LEVELS.contains(moderate(imageUrl).toLowerCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片内容违规，请更换图片");
        }
        String text = recognizeText(data);
        if (StringUtils.hasText(text)) {
            sensitiveWordFilter.assertClean(text);
        }
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
