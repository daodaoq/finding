package com.finding.chat.service;

import com.finding.chat.entity.UserMatchPreference;
import com.finding.chat.vo.ChatApplyVO;
import com.finding.chat.vo.HomeFeedVO;
import com.finding.common.PageVO;

/**
 * 鹊桥服务 —— 相亲推荐、聊天申请、申请管理。
 */
public interface BridgeService {

    /** 分页获取推荐用户（优先同校→同城→近期活跃） */
    PageVO<HomeFeedVO> getRecommendFeed(Long userId, Double lat, Double lng, int page, int size);

    /** 发送聊天申请 */
    void applyChat(Long fromUserId, Long toUserId, String remark);

    /** 我发出的申请列表(status 可空,按状态筛选) */
    PageVO<ChatApplyVO> getSentApplies(Long userId, int page, int size, Integer status);

    /** 我收到的申请列表(status 可空,按状态筛选) */
    PageVO<ChatApplyVO> getReceivedApplies(Long userId, int page, int size, Integer status);

    /** 我收到的待处理(未处理)申请数量 —— 用于「情书」入口角标 */
    long countPendingReceived(Long userId);

    /** 处理聊天申请（通过/拒绝） */
    void handleApply(Long userId, Long applyId, Integer status);

    /** 撤回我发出的待处理申请 */
    void withdrawApply(Long userId, Long applyId);

    /** 获取我的相亲交友偏好(无记录返回默认) */
    UserMatchPreference getMatchPreference(Long userId);

    /** 更新我的相亲交友偏好 */
    void updateMatchPreference(Long userId, UserMatchPreference pref);

    /** 对某候选「不感兴趣」:排除出推荐流并记录跳过事件 */
    void skipUser(Long userId, Long targetUserId);
}
