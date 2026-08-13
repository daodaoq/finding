package com.finding.app.service;

import com.finding.common.word.ReviewResult;
import com.finding.common.word.SensitiveWordFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 图片审核三态判定:风险等级与 OCR 文字 → 拦截/送审/放行。
 */
@ExtendWith(MockitoExtension.class)
class ImageSafetyServiceTest {

    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    private ImageSafetyService service;

    @BeforeEach
    void setUp() {
        service = new ImageSafetyService(sensitiveWordFilter);
    }

    @Test
    void riskLevel_block_returnsBlock() {
        assertEquals(ModerationVerdict.BLOCK, service.classify("block", null).verdict());
        assertEquals(ModerationVerdict.BLOCK, service.classify("HIGH", null).verdict());
    }

    @Test
    void riskLevel_reviewOrMedium_returnsReview() {
        assertEquals(ModerationVerdict.REVIEW, service.classify("review", null).verdict());
        assertEquals(ModerationVerdict.REVIEW, service.classify("medium", null).verdict());
    }

    @Test
    void riskLevel_noneOrLowOrNull_returnsPass() {
        assertEquals(ModerationVerdict.PASS, service.classify("none", null).verdict());
        assertEquals(ModerationVerdict.PASS, service.classify("low", null).verdict());
        assertEquals(ModerationVerdict.PASS, service.classify(null, null).verdict());
    }

    @Test
    void ocrBlockingWord_returnsBlock() {
        when(sensitiveWordFilter.classifyReview(anyString()))
                .thenReturn(new ReviewResult(Set.of("赌"), Set.of()));
        assertEquals(ModerationVerdict.BLOCK, service.classify("none", "加我微信赌一把").verdict());
    }

    @Test
    void ocrReviewWord_returnsReview() {
        when(sensitiveWordFilter.classifyReview(anyString()))
                .thenReturn(new ReviewResult(Set.of(), Set.of("暧昧")));
        assertEquals(ModerationVerdict.REVIEW, service.classify("none", "有点暧昧").verdict());
    }

    @Test
    void ocrClean_returnsPass() {
        when(sensitiveWordFilter.classifyReview(anyString()))
                .thenReturn(new ReviewResult(Set.of(), Set.of()));
        assertEquals(ModerationVerdict.PASS, service.classify("none", "正常文字").verdict());
    }

    @Test
    void ocrBlockingWord_trumpsReviewRiskLevel() {
        when(sensitiveWordFilter.classifyReview(anyString()))
                .thenReturn(new ReviewResult(Set.of("毒"), Set.of()));
        assertEquals(ModerationVerdict.BLOCK, service.classify("review", "卖毒品").verdict());
    }
}
