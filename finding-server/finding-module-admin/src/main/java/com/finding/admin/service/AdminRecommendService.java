package com.finding.admin.service;

import java.util.Map;

/**
 * 管理员 - 相亲推荐行为统计。
 *
 * <p>只统计「今天」(自然日 00:00 起)的四类匿名行为事件:曝光/跳过/申请/通过,
 * 用于运营观察推荐漏斗;不返回任何用户身份信息。</p>
 */
public interface AdminRecommendService {

    /** 今日行为计数:key 为事件类型(expose/skip/apply/approve),value 为条数,按上述顺序返回 */
    Map<String, Object> todayStats();
}
