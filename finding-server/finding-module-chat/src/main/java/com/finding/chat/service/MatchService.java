package com.finding.chat.service;

import com.finding.chat.vo.MatchUserVO;
import com.finding.common.PageVO;

/**
 * 双向心动配对 —— 单向喜欢(心动) + 互相喜欢即配对(match)。
 */
public interface MatchService {

    /** 心动 targetId,若对方也喜欢我则配对成功;返回本次是否产生配对 */
    boolean likeUser(Long userId, Long targetId);

    /** 取消心动;若已配对则同时解除配对 */
    void unlikeUser(Long userId, Long targetId);

    /** 我喜欢的人(发出的心动) */
    PageVO<MatchUserVO> getMyLikes(Long userId, int page, int size);

    /** 喜欢我的人(收到的心动) */
    PageVO<MatchUserVO> getLikesReceived(Long userId, int page, int size);

    /** 互相喜欢(配对)列表 */
    PageVO<MatchUserVO> getMyMatches(Long userId, int page, int size);
}
