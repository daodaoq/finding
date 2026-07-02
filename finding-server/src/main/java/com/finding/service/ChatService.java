package com.finding.service;

import com.finding.dto.MessageSendDTO;
import com.finding.vo.ChatMessageVO;
import com.finding.vo.ConversationVO;
import com.finding.vo.PageVO;

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
}
