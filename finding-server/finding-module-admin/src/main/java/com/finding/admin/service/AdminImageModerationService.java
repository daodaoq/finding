package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 图片审核复核队列。
 *
 * <p>机器送审(verdict=2)的图片进入此队列,由人工放行或删除:
 * 放行仅落复核结果;驳回额外删除 MinIO 已上传对象并通知上传者,
 * 删除失败不阻断复核落库(只记日志)。</p>
 */
public interface AdminImageModerationService {

    /** 待复核队列(verdict=2 送审 且 status=0 待复核) */
    PageVO<Map<String, Object>> reviewQueue(int page, int size);

    /** 复核:pass=true 放行,false 删除图片并通知上传者 */
    void handle(Long adminId, Long id, Map<String, Object> body);
}
