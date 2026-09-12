package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 站内通知与聊天记录审查。
 *
 * <p>写操作对入参做显式校验(缺 targetUserId / 内容为空一律参数错误),
 * 删除消息需存在性检查并记审计。</p>
 */
public interface AdminMessageService {

    /** 给单个用户发系统通知 */
    void sendMessage(Map<String, Object> body);

    /** 给全部正常状态用户广播系统通知 */
    void broadcast(Map<String, Object> body);

    /** 按用户查看私聊消息(可选 otherUserId 只看两人对话) */
    PageVO<Map<String, Object>> chatMessages(Long userId, Long otherUserId, int page, int size);

    /** 删除单条私聊消息(需存在并记审计) */
    void deleteChatMessage(Long adminId, Long id);

    /** 按群/按发送用户查看群聊消息 */
    PageVO<Map<String, Object>> groupMessages(Long groupId, Long userId, int page, int size);

    /** 删除单条群聊消息(需存在并记审计) */
    void deleteGroupMessage(Long adminId, Long id);
}
