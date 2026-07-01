package com.finding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
