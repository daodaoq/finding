package com.finding.app.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.bridge.entity.RecommendEvent;
import com.finding.bridge.mapper.RecommendEventMapper;
import com.finding.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员 - 相亲推荐行为统计(曝光/跳过/申请/通过,匿名)。
 */
@RestController
@RequestMapping("/api/v1/admin/recommend")
@RequiredArgsConstructor
public class AdminRecommendController {

    private final RecommendEventMapper eventMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> map = new LinkedHashMap<>();
        for (String t : List.of("expose", "skip", "apply", "approve")) {
            long c = eventMapper.selectCount(new LambdaQueryWrapper<RecommendEvent>()
                    .eq(RecommendEvent::getEventType, t)
                    .ge(RecommendEvent::getCreatedAt, todayStart));
            map.put(t, c);
        }
        return Result.ok(map);
    }
}
