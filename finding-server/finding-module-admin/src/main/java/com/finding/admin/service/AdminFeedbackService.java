package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 用户反馈/客服工单处理。
 *
 * <p>工单按创建时间倒序分页(可按处理状态筛选),列表批量补全提交人昵称;
 * 处理动作仅允许在「待处理(0)/已处理(1)」之间切换,置为已处理时写入处理时间。</p>
 */
public interface AdminFeedbackService {

    /** 工单列表(含提交人昵称,可按状态筛选) */
    PageVO<Map<String, Object>> listFeedbacks(int page, int size, Integer status);

    /** 标记工单为已处理(1)/重新打开(0) */
    void updateStatus(Long id, Map<String, Integer> body);
}
