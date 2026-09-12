package com.finding.admin.service;

import com.finding.user.entity.User;

import java.util.Collection;
import java.util.Map;

/**
 * 管理端用户信息补全 —— 列表场景按 id 批量取昵称等展示字段。
 *
 * <p>此前每个 controller 各自复制一遍「收集 id → selectBatchIds → 装 Map」的代码(共 11 处),
 * 统一收敛到这里;查不到的 id 不会出现在返回 Map 中,调用方需自带兜底文案。</p>
 */
public interface AdminUserLookupService {

    /** 批量取昵称:key=用户 id,value=昵称;空入参返回空 Map */
    Map<Long, String> nicknamesByIds(Collection<Long> userIds);

    /** 批量取用户实体(需要昵称之外的字段,如手机号时使用) */
    Map<Long, User> usersByIds(Collection<Long> userIds);

    /** 取单个昵称;用户不存在返回 null */
    String nicknameOf(Long userId);
}
