package com.finding.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.entity.Banner;
import com.finding.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员 - 首页轮播管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerMapper bannerMapper;

    @GetMapping("/banners")
    public Result<List<Banner>> listBanners() {
        return Result.ok(bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortOrder)));
    }

    @PostMapping("/banners")
    public Result<Banner> createBanner(@RequestBody Banner banner) {
        bannerMapper.insert(banner);
        return Result.ok(banner);
    }

    @PutMapping("/banners/{id}")
    public Result<Void> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "轮播图不存在");
        banner.setId(id);
        bannerMapper.updateById(banner);
        return Result.ok();
    }

    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        bannerMapper.deleteById(id);
        return Result.ok();
    }
}
