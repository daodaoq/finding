package com.finding.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
