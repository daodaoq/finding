package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.dto.PageQueryDTO;
import com.finding.entity.Conversation;
import com.finding.entity.Message;
import com.finding.entity.User;
import com.finding.mapper.ConversationMapper;
import com.finding.mapper.MessageMapper;
import com.finding.mapper.UserMapper;
import com.finding.service.MessageService;
import com.finding.vo.ConversationVO;
import com.finding.vo.MessageVO;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;

    @Override
    public PageVO<MessageVO> listMessages(Long userId, String type, PageQueryDTO query) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId);
        if (type != null) {
            wrapper.eq(Message::getType, type);
        }
        wrapper.orderByDesc(Message::getCreatedAt);

        Page<Message> page = new Page<>(query.getPage(), query.getSize());
        Page<Message> result = messageMapper.selectPage(page, wrapper);

        List<MessageVO> records = result.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    @Override
    public void markAsRead(Long userId, Long messageId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null || !msg.getToUserId().equals(userId)) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_FOUND);
        }
        msg.setIsRead(1);
        messageMapper.updateById(msg);
    }

    @Override
    public void markAllAsRead(Long userId, String type) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .eq(Message::getIsRead, 0);
        if (type != null) {
            wrapper.eq(Message::getType, type);
        }
        Message update = new Message();
        update.setIsRead(1);
        messageMapper.update(update, wrapper);
    }

    @Override
    public void deleteMessage(Long userId, Long messageId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null || !msg.getToUserId().equals(userId)) {
            throw new BusinessException(ResultCode.MESSAGE_NOT_FOUND);
        }
        messageMapper.deleteById(messageId);
    }

    @Override
    public PageVO<ConversationVO> listConversations(Long userId, PageQueryDTO query) {
        Page<Conversation> page = new Page<>(query.getPage(), query.getSize());
        Page<Conversation> result = conversationMapper.selectPage(page,
                new LambdaQueryWrapper<Conversation>()
                        .and(w -> w.eq(Conversation::getUser1Id, userId)
                                .or().eq(Conversation::getUser2Id, userId))
                        .orderByDesc(Conversation::getLastMessageAt));

        List<ConversationVO> records = result.getRecords().stream()
                .map(c -> toConversationVO(c, userId))
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    private MessageVO toVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setFromUserId(m.getFromUserId());
        vo.setToUserId(m.getToUserId());
        vo.setType(m.getType());
        vo.setContent(m.getContent());
        vo.setRelatedId(m.getRelatedId());
        vo.setIsRead(m.getIsRead());
        vo.setCreatedAt(m.getCreatedAt());

        if (m.getFromUserId() != null) {
            User from = userMapper.selectById(m.getFromUserId());
            if (from != null) {
                vo.setFromUserNickname(from.getNickname());
                vo.setFromUserAvatar(from.getAvatar());
            }
        }
        return vo;
    }

    private ConversationVO toConversationVO(Conversation c, Long currentUserId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(c.getId());
        Long targetId = c.getUser1Id().equals(currentUserId) ? c.getUser2Id() : c.getUser1Id();
        vo.setTargetUserId(targetId);
        vo.setLastMessage(c.getLastMessage());
        vo.setLastMessageAt(c.getLastMessageAt());

        User target = userMapper.selectById(targetId);
        if (target != null) {
            vo.setTargetNickname(target.getNickname());
            vo.setTargetAvatar(target.getAvatar());
        }
        return vo;
    }
}
