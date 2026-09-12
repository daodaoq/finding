package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 学生认证审核。
 *
 * <p>审核人一律取自登录态(adminId 传入),不接受客户端指定 —— 审核记录里的 reviewerId
 * 是审计依据,不能被调用方伪造。审核结果通过事件通知被审核用户。</p>
 */
public interface AdminVerificationService {

    /** 认证记录列表(status 为空查全部) */
    PageVO<Map<String, Object>> listVerifications(int page, int size, Integer status);

    /** 通过认证:认证记录置通过,用户置已实名 */
    void approve(Long adminId, Long id);

    /** 拒绝认证:记录拒绝原因,用户置未通过 */
    void reject(Long adminId, Long id, String comment);
}
