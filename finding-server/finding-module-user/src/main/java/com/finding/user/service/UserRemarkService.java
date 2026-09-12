package com.finding.user.service;

/**
 * 用户备注(私密别名)写入与查询。
 * 展示层的"备注覆盖昵称"统一走 {@link UserRelationshipService}(remarkOf / remarkMap / projectRemark),
 * 业务模块不要各自查 user_remark 表。
 */
public interface UserRemarkService {

    /** 设置/修改备注(已存在则覆盖);remark 为空、超长、含违禁词或给自己设置时抛业务异常 */
    void setRemark(Long userId, Long targetUserId, String remark);

    /** 清除备注(不存在时不报错) */
    void clearRemark(Long userId, Long targetUserId);

    /** 我对某人的备注;无备注返回 null */
    String getRemark(Long userId, Long targetUserId);
}
