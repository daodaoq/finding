package com.finding.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finding.user.entity.UserRemark;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRemarkMapper extends BaseMapper<UserRemark> {

    /**
     * 设置/覆盖备注:命中唯一键时只更新 remark,保留 created_at。
     * 单条语句原子完成,避免"先查后写"的竞态。自定义 SQL 不走 MetaObjectHandler,故显式 NOW()。
     */
    @Insert("INSERT INTO user_remark (user_id, target_user_id, remark, created_at, updated_at) " +
            "VALUES (#{userId}, #{targetUserId}, #{remark}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE remark = VALUES(remark), updated_at = NOW()")
    int upsert(@Param("userId") Long userId,
               @Param("targetUserId") Long targetUserId,
               @Param("remark") String remark);

    /**
     * 反查:用户 userId 是否用 remark 这个别名指代某人(供 @提及 用备注解析目标)。
     * 参数化查询,绝不拼接用户输入。
     */
    @Select("SELECT target_user_id FROM user_remark WHERE user_id = #{userId} AND remark = #{remark} LIMIT 1")
    Long findTargetIdByRemark(@Param("userId") Long userId, @Param("remark") String remark);
}
