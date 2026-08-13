package com.finding.app.service;

/**
 * 图片内容安全判定结果。
 * <p>{@code PASS}放行;{@code BLOCK}拦截(违规,不落库);{@code REVIEW}送审(中等风险,可发布但进入后台复核队列)。</p>
 */
public enum ModerationVerdict {
    PASS, BLOCK, REVIEW
}
