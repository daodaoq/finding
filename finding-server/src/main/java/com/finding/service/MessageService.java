package com.finding.service;

import com.finding.dto.PageQueryDTO;
import com.finding.vo.ConversationVO;
import com.finding.vo.MessageVO;
import com.finding.vo.PageVO;

public interface MessageService {

    PageVO<MessageVO> listMessages(Long userId, String type, PageQueryDTO query);
    long getUnreadCount(Long userId);
    void markAsRead(Long userId, Long messageId);
    void markAllAsRead(Long userId, String type);
    void deleteMessage(Long userId, Long messageId);
    PageVO<ConversationVO> listConversations(Long userId, PageQueryDTO query);
}
