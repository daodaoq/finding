package com.finding.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.post.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
}
