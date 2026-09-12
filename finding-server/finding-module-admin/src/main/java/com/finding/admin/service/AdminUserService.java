package com.finding.admin.service;

import com.finding.common.PageVO;
import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;

import java.util.Map;

/**
 * 管理员 - 用户管理。
 *
 * <p>写操作对入参做类型安全转换与取值白名单(角色仅允许 USER/ADMIN、封禁天数 0~3650),
 * 操作人由调用方以 adminId 传入,便于审计与单测。</p>
 */
public interface AdminUserService {

    /** 用户列表(昵称/手机号模糊搜索) */
    PageVO<Map<String, Object>> listUsers(int page, int size, String keyword);

    /** 新建用户:手机号必填且唯一,返回 {id} */
    Map<String, Object> createUser(Map<String, Object> body);

    /** 用户详情(含完整字段) */
    Map<String, Object> getUserDetail(Long id);

    /** 编辑用户信息(仅更新请求体中出现的字段) */
    void updateUser(Long id, Map<String, Object> body);

    /** 启用/禁用;解封时清空封禁到期时间与原因 */
    void toggleUserStatus(Long id, Map<String, Object> body);

    /** 封禁用户:days>0 封禁 N 天(上限 3650),days=0 永久;发布事件通知在线用户 */
    void banUser(Long adminId, Long id, Map<String, Object> body);

    /** 警告用户一次:落库 + 事件通知 */
    void warnUser(Long adminId, Long id, Map<String, Object> body);

    /** 查看任意用户的情感简历(绕过互换权限,仅管理员可用) */
    UserResume getUserResume(Long id);

    /** 编辑任意用户的情感简历 */
    void updateUserResume(Long id, UserResumeDTO dto);
}
