package com.finding.app.service;

import com.finding.chat.vo.HomeFeedVO;
import com.finding.common.PageVO;

import java.util.List;
import java.util.Map;

public interface HomeService {

    PageVO<HomeFeedVO> getRecommendFeed(Long userId, Double lat, Double lng, int page, int size);
    void likeUser(Long userId, Long targetUserId);
    List<Map<String, Object>> getBanners();
}
