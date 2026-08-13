package com.finding.admin.service;

import java.util.Map;

/** 管理员数据面板 —— 质量与漏斗指标 */
public interface DashboardService {

    /** 质量指标:性别比/认证率/留存率/审核时效 */
    Map<String, Object> quality();
}
