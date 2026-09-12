package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminReportService;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.util.AdminPaging;
import com.finding.chat.entity.Report;
import com.finding.chat.mapper.ReportMapper;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.message.service.MessageService;
import com.finding.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportMapper reportMapper;
    private final AdminUserLookupService userLookupService;
    private final MessageService messageService;
    private final OperationAuditService operationAuditService;

    @Override
    public PageVO<Map<String, Object>> listReports(int page, int size, Integer status) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> result = reportMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量取投诉人 + 被投诉人信息
        Set<Long> userIds = new HashSet<>();
        result.getRecords().forEach(r -> {
            if (r.getFromUserId() != null) userIds.add(r.getFromUserId());
            if (r.getTargetUserId() != null) userIds.add(r.getTargetUserId());
        });
        Map<Long, User> userMap = userLookupService.usersByIds(userIds);

        // 轻量风控计数:被投诉人累计被投诉数 / 投诉人累计投诉数(2 次分组聚合查询)
        Set<Long> targetIds = result.getRecords().stream().map(Report::getTargetUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> fromIds = result.getRecords().stream().map(Report::getFromUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> targetCount = countGrouped(targetIds, true);
        Map<Long, Long> fromCount = countGrouped(fromIds, false);

        List<Map<String, Object>> records = result.getRecords().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("fromUserId", r.getFromUserId());
            map.put("targetUserId", r.getTargetUserId());
            User from = userMap.get(r.getFromUserId());
            User target = userMap.get(r.getTargetUserId());
            map.put("fromNickname", from != null ? from.getNickname() : "用户" + r.getFromUserId());
            map.put("fromAvatar", from != null ? from.getAvatar() : null);
            map.put("targetNickname", target != null ? target.getNickname() : "用户" + r.getTargetUserId());
            map.put("targetAvatar", target != null ? target.getAvatar() : null);
            map.put("reason", r.getReason());
            map.put("evidence", r.getEvidence());
            map.put("status", r.getStatus());
            map.put("handleBy", r.getHandleBy());
            map.put("handleNote", r.getHandleNote());
            map.put("handleTime", r.getHandleTime());
            map.put("roomId", r.getRoomId());
            map.put("targetType", r.getTargetType());
            map.put("targetId", r.getTargetId());
            map.put("contentSnapshot", r.getContentSnapshot());
            map.put("createdAt", r.getCreatedAt());
            map.put("targetReportCount", targetCount.getOrDefault(r.getTargetUserId(), 0L));
            map.put("fromReportCount", fromCount.getOrDefault(r.getFromUserId(), 0L));
            return map;
        }).toList();

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updateReportStatus(Long adminId, Long id, Map<String, Object> body) {
        Report report = reportMapper.selectById(id);
        if (report == null) throw new BusinessException(ResultCode.PARAM_ERROR, "投诉记录不存在");
        Integer status = asInt(body.get("status"));
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能是 1(已处理) 或 2(驳回)");
        }
        String note = body.get("note") != null ? body.get("note").toString() : null;

        report.setStatus(status);
        report.setHandleBy(adminId);
        report.setHandleNote(note);
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);

        // 通知投诉人处理结果
        if (report.getFromUserId() != null) {
            String base = status == 1 ? "你的投诉已处理" : "你的投诉已驳回";
            String content = StringUtils.hasText(note) ? base + "（" + note + "）" : base;
            messageService.notify(adminId, report.getFromUserId(),
                    status == 1 ? "report_handled" : "report_rejected", content, id);
        }
        operationAuditService.record(adminId, "report_handle", "report", report.getId(),
                status == 1 ? "处理投诉" : "驳回投诉", note);
    }

    /**
     * 按被投诉人(target=true)或投诉人(from=false)分组统计累计次数。
     * 单条 GROUP BY 聚合,避免把相关记录全部读入内存再逐行累加。
     */
    private Map<Long, Long> countGrouped(Set<Long> ids, boolean byTarget) {
        Map<Long, Long> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        String column = byTarget ? "target_user_id" : "from_user_id";
        List<Map<String, Object>> rows = reportMapper.selectMaps(new QueryWrapper<Report>()
                .select(column + " AS uid", "COUNT(*) AS cnt")
                .in(column, ids)
                .groupBy(column));
        for (Map<String, Object> row : rows) {
            Object uid = row.get("uid");
            Object cnt = row.get("cnt");
            if (uid instanceof Number u && cnt instanceof Number c) {
                map.put(u.longValue(), c.longValue());
            }
        }
        return map;
    }

    private Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 格式不正确");
        }
    }
}
