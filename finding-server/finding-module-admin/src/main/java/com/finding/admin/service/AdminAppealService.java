package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 申诉处理。
 *
 * <p>处理语义:通过则把被处罚的动态恢复到正常态(review_status 归零、下架态回到发布),
 * 驳回则保留原结果;两种情况都通知申诉人并落审计。</p>
 */
public interface AdminAppealService {

    /** 申诉列表(可按状态过滤,含申诉人昵称) */
    PageVO<Map<String, Object>> listAppeals(int page, int size, Integer status);

    /** 处理申诉:pass=true 通过(重新发布动态),false 驳回(保留原结果) */
    void handle(Long adminId, Long id, Map<String, Object> body);
}
