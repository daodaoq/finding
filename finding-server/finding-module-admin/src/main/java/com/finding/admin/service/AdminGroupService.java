package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 群聊管理。
 *
 * <p>群列表按创建时间倒序分页,并批量补全群主昵称;管理员可修改群名与公告,
 * 解散群时级联删除成员关系与群消息(整体一个事务)。</p>
 */
public interface AdminGroupService {

    /** 群列表(含群主昵称,可按群名模糊搜索) */
    PageVO<Map<String, Object>> listGroups(int page, int size, String keyword);

    /** 编辑群信息(管理员可改一切:群名、公告) */
    void updateGroup(Long id, Map<String, Object> body);

    /** 解散群:级联删除成员关系、群消息、群本身 */
    void disbandGroup(Long id);
}
