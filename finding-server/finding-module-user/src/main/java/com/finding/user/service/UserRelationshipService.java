package com.finding.user.service;

import com.finding.user.vo.UserVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 统一关系权限服务 —— 拉黑/搜索可见性/资料可见性/联系权限的唯一裁决入口。
 *
 * 业务模块(用户/聊天/搭子/搜索/推荐)不得各自重复编写 user_block / user_settings 查询,
 * 统一调用本服务。所有"能否发现/查看/关注/联系"判断均由服务端裁决,前端只做展示投影。
 */
public interface UserRelationshipService {

    /** 任一方拉黑对方 → true(双向) */
    boolean isBlockedEitherWay(Long userId, Long targetId);

    /** 与当前用户存在拉黑关系的所有对方用户 id(双向),用于 SQL 级 notIn 过滤 */
    Set<Long> blockedUserIds(Long userId);

    /** 从 targetIds 中过滤出未被当前用户双向拉黑的部分 */
    List<Long> filterNotBlocked(Long userId, Collection<Long> targetIds);

    /**
     * 发现权限:非本人 + 未双向拉黑 + 目标允许被搜索(searchable=1) + 目标账号正常(status=1)。
     * 用于"用户搜索"与"相亲推荐"的候选资格过滤。
     */
    boolean canDiscover(Long visitorId, Long targetId);

    /**
     * 查看详细资料权限:本人 → true;双向拉黑 → false;
     * 目标资料可见性为"仅已互换"(profile_visible=2) → 要求信息互换已通过(任一方向 approved);否则 true。
     */
    boolean canViewDetailedProfile(Long visitorId, Long targetId);

    /** 关注权限:未双向拉黑(且非本人) */
    boolean canFollow(Long visitorId, Long targetId);

    /** 发起聊天申请权限:未双向拉黑 且 对方联系权限允许(friend_add_mode != 2) */
    boolean canApplyChat(Long visitorId, Long targetId);

    /**
     * 按资料可见性投影 UserVO:不可查看详细资料时,隐藏性别/城市/签名(仅保留公开资料)。
     */
    default void projectDetailedFields(Long visitorId, Long ownerId, UserVO vo) {
        if (!canViewDetailedProfile(visitorId, ownerId)) {
            vo.setGender(null);
            vo.setSignature(null);
            vo.setCity(null);
        }
    }
}
