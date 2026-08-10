package com.finding.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.app.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
