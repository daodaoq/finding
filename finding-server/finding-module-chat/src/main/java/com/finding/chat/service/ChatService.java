package com.finding.chat.service;

import com.finding.chat.dto.MessageSendDTO;
import com.finding.chat.vo.ChatMessageVO;
import com.finding.chat.vo.ConversationSettingsVO;
import com.finding.message.vo.ConversationVO;
import com.finding.common.PageVO;

import java.util.List;

/**
 * 聊天服务 —— 私聊消息收发、会话管理、消息历史。
 * 基于 MallChat Room 模型：所有消息和会话围绕 room 展开。
 */
public interface ChatService {

    /** 创建或获取与另一个用户的会话（返回 roomId） */
    ConversationVO getOrCreateConversation(Long userId, Long targetUserId);

    /** 获取当前用户的所有会话列表 */
    List<ConversationVO> listConversations(Long userId);

    /** 发送私聊消息（REST方式，也供 WebSocket 调用） */
    ConversationVO sendMessage(Long userId, MessageSendDTO dto);

    /** 获取会话消息历史（id=room_id，游标分页） */
    PageVO<ChatMessageVO> getMessageHistory(Long userId, Long roomId, Long lastId, int size);

    /** 标记会话消息为已读（id=room_id） */
    void markConversationRead(Long userId, Long roomId);

    /** 获取会话设置(置顶/免打扰/聊天背景) */
    ConversationSettingsVO getConversationSettings(Long userId, Long roomId);

    /** 更新会话设置(置顶/免打扰/聊天背景) */
    void updateConversationSettings(Long userId, Long roomId, Boolean pinned, Boolean muted, String background);

    /** 搜索会话内的聊天记录(按内容模糊匹配) */
    PageVO<ChatMessageVO> searchMessages(Long userId, Long roomId, String keyword, int size);

    /** 清空会话聊天记录(双方) */
    void clearMessages(Long userId, Long roomId);

    /** 投诉用户 */
    void reportUser(Long fromUserId, Long toUserId, Long roomId, String reason);

    /** 撤回自己发送的消息(2分钟内),并 WS 同步双方 */
    void recallMessage(Long userId, Long messageId);
}
