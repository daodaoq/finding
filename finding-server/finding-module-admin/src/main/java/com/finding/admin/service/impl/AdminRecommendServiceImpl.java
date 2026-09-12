package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.admin.service.AdminRecommendService;
import com.finding.bridge.entity.RecommendEvent;
import com.finding.bridge.mapper.RecommendEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminRecommendServiceImpl implements AdminRecommendService {

    /** 统计的事件类型(返回顺序即此顺序) */
    private static final List<String> EVENT_TYPES = List.of("expose", "skip", "apply", "approve");

    private final RecommendEventMapper eventMapper;

    @Override
    public Map<String, Object> todayStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> map = new LinkedHashMap<>();
        for (String t : EVENT_TYPES) {
            long c = eventMapper.selectCount(new LambdaQueryWrapper<RecommendEvent>()
                    .eq(RecommendEvent::getEventType, t)
                    .ge(RecommendEvent::getCreatedAt, todayStart));
            map.put(t, c);
        }
        return map;
    }
}
