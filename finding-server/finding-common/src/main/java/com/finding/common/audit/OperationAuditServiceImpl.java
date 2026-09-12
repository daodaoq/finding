package com.finding.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationAuditServiceImpl implements OperationAuditService {

    private final OperationLogMapper operationLogMapper;

    @Override
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
