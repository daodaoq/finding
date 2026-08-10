package com.finding.common.word;

/**
 * 违禁词规则 —— 词 + 动作。
 * <p>action: 0=拦截(命中即拒绝发布) 1=送审(命中进入审核队列,由管理员裁决)</p>
 */
public record SensitiveWordRule(String word, int action) {
}
