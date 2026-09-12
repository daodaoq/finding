package com.finding.admin.service;

import java.util.Map;

/** 管理员数据面板 —— 概览统计、趋势与质量指标 */
public interface AdminDashboardService {

    /** 概览:用户/动态/搭子/认证/举报/群聊的即时计数 */
    Map<String, Object> summary();

    /** 近 N 天趋势(注册/动态/搭子/活跃用户),N 夹紧到 3~30 */
    Map<String, Object> trend(int days);

    /** 质量指标:性别比/认证率/留存率/审核时效 */
    Map<String, Object> quality();
}
