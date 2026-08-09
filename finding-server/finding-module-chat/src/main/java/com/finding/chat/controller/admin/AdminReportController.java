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
import com.finding.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            map.put("status", r.getStatus());
            map.put("roomId", r.getRoomId());
            map.put("targetType", r.getTargetType());
            map.put("targetId", r.getTargetId());
            map.put("contentSnapshot", r.getContentSnapshot());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).toList();

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 处理投诉：status=1 已处理，status=2 驳回 */
    @PutMapping("/reports/{id}/status")
    public Result<Void> updateReportStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Report report = reportMapper.selectById(id);
        if (report == null) throw new BusinessException(ResultCode.PARAM_ERROR, "投诉记录不存在");
        Integer status = body.get("status");
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能是 1(已处理) 或 2(驳回)");
        }
        report.setStatus(status);
        reportMapper.updateById(report);
        return Result.ok();
    }
}
