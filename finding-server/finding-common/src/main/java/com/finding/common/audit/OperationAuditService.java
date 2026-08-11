package com.finding.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 敏感操作审计 —— 封禁/举报处理/内容审核/搭子处置等写审计表,
 * 供线上问题复盘与合规追溯。审计失败不阻断主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationAuditService {

    private final OperationLogMapper operationLogMapper;

    public void record(Long operatorId, String action, String targetType, Long targetId, String detail, String result) {
        try {
            OperationLog l = new OperationLog();
            l.setOperatorId(operatorId);
            l.setAction(action);
            l.setTargetType(targetType);
            l.setTargetId(targetId);
            l.setDetail(detail);
            l.setResult(result);
            operationLogMapper.insert(l);
        } catch (Exception e) {
            log.warn("审计记录失败 action={} targetType={} targetId={}", action, targetType, targetId, e);
        }
    }
}
