package com.finding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
