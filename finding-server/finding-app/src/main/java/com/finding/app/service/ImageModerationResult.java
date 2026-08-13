package com.finding.app.service;

/**
 * 图片审核结果 —— 风险等级、OCR 提取文字与最终判定。
 */
public record ImageModerationResult(String riskLevel, String ocrText, ModerationVerdict verdict) {
}
