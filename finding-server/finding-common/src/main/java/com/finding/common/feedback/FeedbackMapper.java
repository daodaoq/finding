package com.finding.common.feedback;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.common.feedback.Feedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
