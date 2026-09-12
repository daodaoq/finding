package com.finding.common.audit;

/**
 * 敏感操作审计 —— 封禁/举报处理/内容审核/搭子处置等写审计表,
 * 供线上问题复盘与合规追溯。审计失败不阻断主流程。
 */
public interface OperationAuditService {

    /** 记录一条审计日志(内部吞掉异常,不影响主流程) */
    void record(Long operatorId, String action, String targetType, Long targetId, String detail, String result);
}
