package com.finding.app.controller;

import com.finding.common.Result;
import com.finding.user.security.JwtInterceptor;
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
}
