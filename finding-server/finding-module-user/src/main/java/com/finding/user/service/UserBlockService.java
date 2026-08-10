package com.finding.user.service;

import java.util.Map;

/**
 * 拉黑/屏蔽 —— 防骚扰。
 */
public interface UserBlockService {

    /** 拉黑(重复拉黑/拉黑自己/用户不存在会抛错) */
    void block(Long userId, Long targetUserId);

    /** 解除拉黑 */
    void unblock(Long userId, Long targetUserId);

    /** userId 是否拉黑了 targetUserId */
    boolean isBlocked(Long userId, Long targetUserId);

    /** 拉黑状态:blocked=我拉黑了对方, blockedBy=对方拉黑了我 */
    Map<String, Boolean> blockStatus(Long userId, Long targetUserId);
}
