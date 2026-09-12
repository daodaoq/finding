package com.finding.admin.service;

import com.finding.common.content.Banner;

import java.util.List;

/**
 * 管理员 - 首页轮播管理。
 *
 * <p>列表按下发顺序(sort_order)升序返回;新增直接落库,更新前校验存在性并以路径 id 覆盖实体 id。</p>
 */
public interface AdminBannerService {

    /** 轮播列表(按下发顺序升序) */
    List<Banner> listBanners();

    /** 新增轮播,返回落库后的实体 */
    Banner createBanner(Banner banner);

    /** 更新轮播(以路径 id 为准) */
    void updateBanner(Long id, Banner banner);

    /** 删除轮播 */
    void deleteBanner(Long id);
}
