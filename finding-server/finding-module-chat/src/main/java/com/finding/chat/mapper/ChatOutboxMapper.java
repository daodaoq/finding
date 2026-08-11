package com.finding.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.chat.entity.ChatOutbox;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatOutboxMapper extends BaseMapper<ChatOutbox> {
}
