package com.finding.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.post.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
