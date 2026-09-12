package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 操作审计日志查看。
 *
 * <p>敏感操作(封禁/举报处理/内容审核等)写入 operation_log,此处提供只读分页查询与筛选;
 * 列表按操作者 id 批量补全昵称,避免 N+1。</p>
 */
public interface AdminAuditService {

    /** 审计日志分页查询(action/targetType/operatorId/detail 关键字筛选,含操作者昵称) */
    PageVO<Map<String, Object>> listLogs(int page, int size, String action, String targetType,
                                         Long operatorId, String keyword);
}
