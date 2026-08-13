package com.finding.post.service;

import java.util.List;
import java.util.Map;

/** 内容申诉 —— 用户对被拒/下架内容发起申诉与查询 */
public interface AppealService {

    /** 对审核未通过或被下架的动态发起申诉(同一内容有次数上限) */
    void appeal(Long userId, Long postId, String reason);

    /** 我的申诉记录 */
    List<Map<String, Object>> myAppeals(Long userId);
}
