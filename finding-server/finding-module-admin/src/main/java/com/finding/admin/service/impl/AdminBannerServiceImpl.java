package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.admin.service.AdminBannerService;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.content.Banner;
import com.finding.common.content.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBannerServiceImpl implements AdminBannerService {

    private final BannerMapper bannerMapper;

    @Override
    public List<Banner> listBanners() {
        return bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortOrder));
    }

    @Override
    public Banner createBanner(Banner banner) {
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    public void updateBanner(Long id, Banner banner) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) throw new BusinessException(ResultCode.PARAM_ERROR, "轮播图不存在");
        banner.setId(id);
        bannerMapper.updateById(banner);
    }

    @Override
    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
    }
}
