package com.finding.message.service;

import com.finding.common.PageQueryDTO;
import com.finding.message.vo.ConversationVO;
import com.finding.message.vo.MessageVO;
import com.finding.common.PageVO;

public interface MessageService {

    PageVO<MessageVO> listMessages(Long userId, String type, PageQueryDTO query);
    long getUnreadCount(Long userId);
    void markAsRead(Long userId, Long messageId);
    void markAllAsRead(Long userId, String type);
    void deleteMessage(Long userId, Long messageId);
    PageVO<ConversationVO> listConversations(Long userId, PageQueryDTO query);

    /**
     * 写入一条站内通知。
     * 供其他业务模块(动态点赞/评论、搭子申请、聊天申请等)调用,
     * 避免各模块直接操作 message 表。
     */
    void notify(Long fromUserId, Long toUserId, String type, String content, Long relatedId);
}
