package com.finding.bridge.service;

import com.finding.bridge.dto.UserCardConfigDTO;
import com.finding.bridge.entity.UserCardConfig;
import com.finding.bridge.entity.UserMatchPreference;
import com.finding.bridge.vo.ChatApplyVO;
import com.finding.bridge.vo.HomeFeedVO;
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

    /** 获取我的相识卡片展示配置(无记录返回全开默认) */
    UserCardConfig getCardConfig(Long userId);

    /** 更新我的相识卡片展示配置(不存在则插入) */
    void updateCardConfig(Long userId, UserCardConfigDTO dto);

    /** 预览我的卡片(别人视角,按我的配置裁剪字段) */
    HomeFeedVO previewMyCard(Long userId);
}
