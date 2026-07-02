package com.finding.service;

import com.finding.vo.ChatApplyVO;
import com.finding.vo.HomeFeedVO;
import com.finding.vo.PageVO;

/**
 * 鹊桥服务 —— 相亲推荐、聊天申请、申请管理。
 */
public interface BridgeService {

    /** 分页获取推荐用户（优先同校→同城→近期活跃） */
    PageVO<HomeFeedVO> getRecommendFeed(Long userId, Double lat, Double lng, int page, int size);

    /** 发送聊天申请 */
    void applyChat(Long fromUserId, Long toUserId, String remark);

    /** 我发出的申请列表 */
    PageVO<ChatApplyVO> getSentApplies(Long userId, int page, int size);

    /** 我收到的申请列表 */
    PageVO<ChatApplyVO> getReceivedApplies(Long userId, int page, int size);

    /** 处理聊天申请（通过/拒绝） */
    void handleApply(Long userId, Long applyId, Integer status);
}
