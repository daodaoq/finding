package com.finding.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.audit.OperationLog;
import com.finding.common.audit.OperationLogMapper;
import com.finding.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员 - 操作审计日志查看。
 * 敏感操作(封禁/举报处理/内容审核等)写入 operation_log,此处提供分页查询与筛选。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuditController {

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    @GetMapping("/audit-logs")
    public Result<PageVO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String keyword) {

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
        Map<Long, String> nickMap = new HashMap<>();
        if (!opIds.isEmpty()) {
            userMapper.selectBatchIds(opIds).forEach(u -> nickMap.put(u.getId(), u.getNickname()));
        }

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

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }
}
