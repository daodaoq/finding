package com.finding.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.Result;
import com.finding.user.security.JwtInterceptor;
import com.finding.app.entity.SystemAnnouncement;
import com.finding.app.mapper.SystemAnnouncementMapper;
import com.finding.app.service.HomeService;
import com.finding.chat.vo.HomeFeedVO;
import com.finding.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final SystemAnnouncementMapper announcementMapper;

    @GetMapping("/feed")
    public Result<PageVO<HomeFeedVO>> feed(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Double lat,
                                            @RequestParam(required = false) Double lng) {
        return Result.ok(homeService.getRecommendFeed(JwtInterceptor.getCurrentUserId(), lat, lng, page, size));
    }

    @GetMapping("/banners")
    public Result<List<Map<String, Object>>> banners() {
        return Result.ok(homeService.getBanners());
    }

    /** 获取 afterId 之后的系统公告列表(客户端启动时补拉全部未读,配合 WS 实时推送) */
    @GetMapping("/announcements")
    public Result<List<SystemAnnouncement>> listAnnouncements(
            @RequestParam(defaultValue = "0") Long afterId) {
        return Result.ok(announcementMapper.selectList(
                new LambdaQueryWrapper<SystemAnnouncement>()
                        .gt(SystemAnnouncement::getId, afterId)
                        .orderByAsc(SystemAnnouncement::getId)));
    }
}
