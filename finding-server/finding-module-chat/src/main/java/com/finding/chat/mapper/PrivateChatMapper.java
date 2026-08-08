package com.finding.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.chat.entity.PrivateChat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrivateChatMapper extends BaseMapper<PrivateChat> {
}
