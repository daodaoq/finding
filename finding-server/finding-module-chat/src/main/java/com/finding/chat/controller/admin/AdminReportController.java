package com.finding.chat.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.chat.entity.Report;
import com.finding.chat.mapper.ReportMapper;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.security.JwtInterceptor;
import com.finding.message.service.MessageService;
import com.finding.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员 - 投诉管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @GetMapping("/reports")
    public Result<PageVO<Map<String, Object>>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> result = reportMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量取投诉人 + 被投诉人信息
        Set<Long> userIds = new HashSet<>();
        result.getRecords().forEach(r -> {
            userIds.add(r.getFromUserId());
            userIds.add(r.getTargetUserId());
        });
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }

        // 轻量风控计数:被投诉人累计被投诉数 / 投诉人累计投诉数(2 次分组查询)
        Set<Long> targetIds = result.getRecords().stream().map(Report::getTargetUserId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Set<Long> fromIds = result.getRecords().stream().map(Report::getFromUserId)
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
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

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 处理投诉：status=1 已处理，status=2 驳回；记录处理人/时间/意见并通知投诉人 */
    @PutMapping("/reports/{id}/status")
    public Result<Void> updateReportStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Report report = reportMapper.selectById(id);
        if (report == null) throw new BusinessException(ResultCode.PARAM_ERROR, "投诉记录不存在");
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能是 1(已处理) 或 2(驳回)");
        }
        String note = body.get("note") != null ? body.get("note").toString() : null;
        Long adminId = JwtInterceptor.getCurrentUserId();

        report.setStatus(status);
        report.setHandleBy(adminId);
        report.setHandleNote(note);
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);

        // 通知投诉人处理结果
        if (report.getFromUserId() != null) {
            String base = status == 1 ? "你的投诉已处理" : "你的投诉已驳回";
            String content = note != null && !note.isBlank() ? base + "（" + note + "）" : base;
            messageService.notify(adminId, report.getFromUserId(),
                    status == 1 ? "report_handled" : "report_rejected", content, id);
        }
        return Result.ok();
    }

    /** 按被投诉人(target=true)或投诉人(from=false)分组统计累计次数 */
    private Map<Long, Long> countGrouped(Set<Long> ids, boolean byTarget) {
        Map<Long, Long> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        LambdaQueryWrapper<Report> w = new LambdaQueryWrapper<>();
        if (byTarget) w.in(Report::getTargetUserId, ids);
        else w.in(Report::getFromUserId, ids);
        for (Report r : reportMapper.selectList(w)) {
            Long key = byTarget ? r.getTargetUserId() : r.getFromUserId();
            map.merge(key, 1L, Long::sum);
        }
        return map;
    }
}
