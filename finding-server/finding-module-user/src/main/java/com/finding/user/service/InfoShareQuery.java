package com.finding.user.service;

/**
 * 信息互换状态查询端口(接口)。
 *
 * user 模块是底层叶子模块,不能反向依赖 chat 模块。
 * 该接口由 chat 模块提供实现(InfoShareAdapter),在装配时通过 Spring 注入,
 * 从而在不产生编译期环依赖的前提下,让"情感简历"查看能校验是否已互换信息。
 */
public interface InfoShareQuery {

    /** 无记录 */
    int STATUS_NONE = 0;
    /** 有记录处于待处理(pending) */
    int STATUS_PENDING = 1;
    /** 已互换(approved) */
    int STATUS_APPROVED = 2;
    /** 已拒绝(rejected) */
    int STATUS_REJECTED = 3;

    /**
     * 查询两位用户之间的信息互换状态(任一方向最近一次)。
     *
     * @return {@link #STATUS_NONE} / {@link #STATUS_PENDING} / {@link #STATUS_APPROVED} / {@link #STATUS_REJECTED}
     */
    int getShareStatus(Long uidA, Long uidB);

    /** 是否已互相交换信息(任一方向 approved) */
    default boolean hasApprovedShare(Long uidA, Long uidB) {
        return getShareStatus(uidA, uidB) == STATUS_APPROVED;
    }
}
