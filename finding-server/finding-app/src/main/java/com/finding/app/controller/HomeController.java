package com.finding.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.user.security.JwtInterceptor;
import com.finding.app.entity.SystemAnnouncement;
import com.finding.app.mapper.SystemAnnouncementMapper;
import com.finding.app.service.HomeService;
import com.finding.bridge.vo.HomeFeedVO;
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
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        if (page < 1 || size < 1 || size > 50) {
            return Result.error(ResultCode.PARAM_ERROR, "分页参数不合法: page>=1, size 1-50");
        }
        return Result.ok(homeService.getRecommendFeed(userId, lat, lng, page, size));
    }

    @GetMapping("/banners")
    public Result<List<Map<String, Object>>> banners() {
        return Result.ok(homeService.getBanners());
    }

    /** 获取 afterId 之后的普通公告列表(客户端启动时补拉未读,配合 WS 实时推送;永久公告走顶部横条,不进弹窗) */
    @GetMapping("/announcements")
    public Result<List<SystemAnnouncement>> listAnnouncements(
            @RequestParam(defaultValue = "0") Long afterId) {
        return Result.ok(announcementMapper.selectList(
                new LambdaQueryWrapper<SystemAnnouncement>()
                        .eq(SystemAnnouncement::getType, 1)
                        .eq(SystemAnnouncement::getStatus, 1)
                        .gt(SystemAnnouncement::getId, afterId)
                        .orderByAsc(SystemAnnouncement::getId)));
    }

    /** 当前生效的永久展示公告(顶部悬浮横条),无则 data=null */
    @GetMapping("/permanent-announcements")
    public Result<SystemAnnouncement> permanentAnnouncement() {
        SystemAnnouncement one = announcementMapper.selectOne(
                new LambdaQueryWrapper<SystemAnnouncement>()
                        .eq(SystemAnnouncement::getType, 2)
                        .eq(SystemAnnouncement::getStatus, 1)
                        .orderByDesc(SystemAnnouncement::getCreatedAt)
                        .last("LIMIT 1"));
        return Result.ok(one);
    }
}
