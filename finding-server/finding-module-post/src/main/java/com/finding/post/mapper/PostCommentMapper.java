package com.finding.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.post.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {

    /** 批量统计各动态的正常评论数(替换逐条 selectCount 的 N+1) */
    @Select("<script>" +
            "SELECT post_id AS postId, COUNT(*) AS cnt FROM post_comment " +
            "WHERE status = 0 AND post_id IN " +
            "<foreach collection='postIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach> " +
            "GROUP BY post_id" +
            "</script>")
    List<Map<String, Object>> countByPosts(@Param("postIds") List<Long> postIds);
}
