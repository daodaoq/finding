package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminAuditService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.PageVO;
import com.finding.common.audit.OperationLog;
import com.finding.common.audit.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

    private final OperationLogMapper operationLogMapper;
    private final AdminUserLookupService userLookupService;

    @Override
    public PageVO<Map<String, Object>> listLogs(int page, int size, String action, String targetType,
                                                Long operatorId, String keyword) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(StringUtils.hasText(action), OperationLog::getAction, action)
                .eq(StringUtils.hasText(targetType), OperationLog::getTargetType, targetType)
                .eq(operatorId != null, OperationLog::getOperatorId, operatorId)
                .like(StringUtils.hasText(keyword), OperationLog::getDetail, keyword)
                .orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量解析操作者昵称(避免 N+1)
        Set<Long> opIds = new HashSet<>();
        result.getRecords().forEach(l -> {
            if (l.getOperatorId() != null) opIds.add(l.getOperatorId());
        });
        Map<Long, String> nickMap = userLookupService.nicknamesByIds(opIds);

        List<Map<String, Object>> records = result.getRecords().stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("operatorId", l.getOperatorId());
            m.put("operatorNickname", l.getOperatorId() != null ? nickMap.getOrDefault(l.getOperatorId(), "") : "");
            m.put("action", l.getAction());
            m.put("targetType", l.getTargetType());
            m.put("targetId", l.getTargetId());
            m.put("detail", l.getDetail());
            m.put("result", l.getResult());
            m.put("createdAt", l.getCreatedAt());
            return m;
        }).toList();

        return PageVO.of(records, result.getTotal(), page, size);
    }
}
