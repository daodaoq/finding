package com.finding.admin.service;

import com.finding.common.PageVO;
import com.finding.mate.dto.AdminMateReviewDTO;
import com.finding.mate.dto.AdminMateStatusDTO;
import com.finding.mate.dto.AdminMateUpdateDTO;

import java.util.Map;

/**
 * 管理员 - 搭子邀约管理。
 *
 * <p>状态流转统一收敛:{@code updateMateStatus/closeMate/cancelMate/deleteMate} 都只允许
 * 「进行中 → 已取消/已关闭」,差异仅在通知文案与是否连带作废已通过成员。</p>
 */
public interface AdminMateService {

    /** 邀约列表(含发起人昵称与分类文案) */
    PageVO<Map<String, Object>> listMates(int page, int size, String keyword, Integer status);

    /** 编辑邀约:人数下限、活动时间、内容清洗与违禁词校验 */
    void updateMate(Long id, AdminMateUpdateDTO body);

    /** 下架/关闭(状态由请求体指定,仅允许取消或关闭) */
    void updateMateStatus(Long adminId, Long id, AdminMateStatusDTO body);

    /** 关闭活动(通知作者与报名者,已通过成员保留) */
    void closeMate(Long adminId, Long id);

    /** 取消活动(连带作废已通过成员并通知) */
    void cancelMate(Long adminId, Long id);

    /** 待审核队列 */
    PageVO<Map<String, Object>> reviewQueue(int page, int size);

    /** 审核处理:通过发布 / 拒绝并通知作者 */
    void reviewMate(Long adminId, Long id, AdminMateReviewDTO body);

    /** 软删除(保留参与与审计记录) */
    void deleteMate(Long adminId, Long id);
}
