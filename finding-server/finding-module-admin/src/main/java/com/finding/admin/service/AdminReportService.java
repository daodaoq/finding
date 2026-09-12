package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 投诉管理。
 *
 * <p>列表附带轻量风控计数(被投诉人累计被投诉数 / 投诉人累计投诉数),
 * 使用分组聚合查询,不再把相关记录全量读入内存再计数。</p>
 */
public interface AdminReportService {

    /** 投诉列表(status 为空查全部) */
    PageVO<Map<String, Object>> listReports(int page, int size, Integer status);

    /** 处理投诉:status=1 已处理 / 2 驳回;记录处理人与意见并通知投诉人 */
    void updateReportStatus(Long adminId, Long id, Map<String, Object> body);
}
