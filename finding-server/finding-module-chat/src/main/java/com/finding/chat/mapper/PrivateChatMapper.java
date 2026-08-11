package com.finding.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.chat.entity.PrivateChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PrivateChatMapper extends BaseMapper<PrivateChat> {

    /**
     * 批量查询每个房间最后一条"当前用户可见"消息(替换 N+1 循环)。
     * 窗口函数取每房间最新一条;join room_friend 按 (uid1=uid 且未隐藏) 或 (uid2=uid 且未隐藏) 处理单侧清空。
     */
    @Select("<script>" +
            "SELECT p.id, p.room_id, p.from_user_id, p.to_user_id, p.content, p.message_type, p.created_at " +
            "FROM ( " +
            "  SELECT pc.id, pc.room_id, pc.from_user_id, pc.to_user_id, pc.content, pc.message_type, pc.created_at, " +
            "         ROW_NUMBER() OVER (PARTITION BY pc.room_id ORDER BY pc.id DESC) AS rn " +
            "  FROM private_chat pc " +
            "  JOIN room_friend rf ON rf.room_id = pc.room_id " +
            "    AND ((rf.uid1 = #{uid} AND pc.uid1_hidden = 0) OR (rf.uid2 = #{uid} AND pc.uid2_hidden = 0)) " +
            "  WHERE pc.room_id IN " +
            "    <foreach collection='roomIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach> " +
            ") p WHERE p.rn = 1" +
            "</script>")
    List<PrivateChat> selectLastVisibleMessageByRoom(@Param("uid") Long uid, @Param("roomIds") List<Long> roomIds);

    /** 批量统计每个房间的未读数(仅当前用户可见的未读),替换 N+1 循环 */
    @Select("<script>" +
            "SELECT pc.room_id AS roomId, COUNT(*) AS cnt " +
            "FROM private_chat pc " +
            "JOIN room_friend rf ON rf.room_id = pc.room_id " +
            "  AND ((rf.uid1 = #{uid} AND pc.uid1_hidden = 0) OR (rf.uid2 = #{uid} AND pc.uid2_hidden = 0)) " +
            "WHERE pc.to_user_id = #{uid} AND pc.is_read = 0 AND pc.room_id IN " +
            "  <foreach collection='roomIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach> " +
            "GROUP BY pc.room_id" +
            "</script>")
    List<Map<String, Object>> countUnreadByRoom(@Param("uid") Long uid, @Param("roomIds") List<Long> roomIds);
}
