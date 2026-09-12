package com.finding.group.service;

import com.finding.common.PageVO;
import com.finding.group.vo.GroupChatVO;
import com.finding.group.vo.GroupMessageVO;

import java.util.List;
import java.util.Map;

/**
 * 群聊服务 —— 建群/成员管理/消息收发/历史与公告。
 */
public interface GroupChatService {

    /** 创建群聊 */
    GroupChatVO createGroup(Long ownerId, String name, List<Long> memberIds);

    /** 我参与的群列表 */
    List<GroupChatVO> listMyGroups(Long userId);

    /** 标记群消息已读 */
    void markRead(Long groupId, Long userId);

    /** 群详情(含成员) */
    GroupChatVO getGroupDetail(Long groupId, Long userId);

    /** 发送群消息(落库 + WS 推送) */
    GroupMessageVO sendMessage(Long groupId, Long fromUserId, String content, String messageType);

    /** 群消息历史(分页) */
    PageVO<GroupMessageVO> getMessageHistory(Long groupId, Long userId, int page, int size);

    /** 可邀请入群的用户 */
    List<Map<String, Object>> getInvitableUsers(Long userId, Long groupId);

    /** 拉人入群 */
    void addMembers(Long operatorId, Long groupId, List<Long> userIds);

    /** 移除成员 */
    void removeMember(Long operatorId, Long groupId, Long targetUserId);

    /** 退群(群主则解散) */
    void leaveOrDisband(Long userId, Long groupId);

    /** 更新群公告 */
    void updateAnnouncement(Long groupId, Long userId, String announcement);

    /** 撤回群消息 */
    void recallMessage(Long groupId, Long userId, Long messageId);
}
