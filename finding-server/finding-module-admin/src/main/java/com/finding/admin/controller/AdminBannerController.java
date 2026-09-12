package com.finding.admin.controller;

import com.finding.admin.service.AdminBannerService;
import com.finding.common.Result;
import com.finding.common.content.Banner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员 - 首页轮播管理(业务逻辑见 {@link AdminBannerService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    @GetMapping("/banners")
    public Result<List<Banner>> listBanners() {
        return Result.ok(adminBannerService.listBanners());
    }

    @PostMapping("/banners")
    public Result<Banner> createBanner(@RequestBody Banner banner) {
        return Result.ok(adminBannerService.createBanner(banner));
    }

    @PutMapping("/banners/{id}")
    public Result<Void> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        adminBannerService.updateBanner(id, banner);
        return Result.ok();
    }

    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        adminBannerService.deleteBanner(id);
        return Result.ok();
    }
}
