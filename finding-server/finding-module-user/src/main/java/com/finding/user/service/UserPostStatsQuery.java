package com.finding.user.service;

/**
 * 用户动态数查询端口(接口)。
 *
 * user 模块是底层叶子模块,不能反向依赖 post 模块。
 * 该接口由 post 模块提供实现(UserPostStatsAdapter),在装配时通过 Spring 注入,
 * 从而在不产生编译期环依赖的前提下,让用户资料里能带上"动态数"。
 */
public interface UserPostStatsQuery {

    /** 统计某用户"可见(已发布)"的动态数量 */
    int countPosts(Long userId);
}
