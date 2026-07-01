package com.finding.service;

import com.finding.dto.MessageSendDTO;
import com.finding.vo.ConversationVO;
import com.finding.vo.PageVO;

import java.util.List;

/**
 * 聊天服务 —— 私聊消息收发、会话管理、消息历史。
 */
public interface ChatService {

    /** 创建或获取与另一个用户的会话 */
    ConversationVO getOrCreateConversation(Long userId, Long targetUserId);

    /** 获取当前用户的所有会话列表 */
    List<ConversationVO> listConversations(Long userId);

    /** 发送私聊消息（REST方式，也供 WebSocket 调用） */
    ConversationVO sendMessage(Long userId, MessageSendDTO dto);

    /** 获取会话消息历史（游标分页） */
    PageVO<ConversationVO> getMessageHistory(Long userId, Long conversationId, Long lastId, int size);

    /** 标记会话消息为已读 */
    void markConversationRead(Long userId, Long conversationId);
}
