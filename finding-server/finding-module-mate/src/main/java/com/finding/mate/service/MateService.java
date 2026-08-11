package com.finding.mate.service;

import com.finding.mate.dto.MateCreateDTO;
import com.finding.mate.dto.MateQueryDTO;
import com.finding.mate.vo.MateVO;
import com.finding.common.PageVO;

import java.util.Map;

public interface MateService {

    PageVO<MateVO> listInvitations(MateQueryDTO query, Long currentUserId);
    MateVO getInvitationDetail(Long id, Long currentUserId);
    MateVO createInvitation(Long userId, MateCreateDTO dto);
    void updateInvitation(Long userId, Long id, MateCreateDTO dto);
    void cancelInvitation(Long userId, Long id);
    void closeInvitation(Long userId, Long id);
    void joinInvitation(Long userId, Long id, String message);
    void leaveInvitation(Long userId, Long id);
    void handleJoinRequest(Long userId, Long id, Long participantId, boolean accept);

    PageVO<MateVO> getMyInvitations(Long userId, MateQueryDTO query);
    PageVO<MateVO> getMyJoinedInvitations(Long userId, MateQueryDTO query);

    /** 我的全部申请记录(含待审核/已通过/被拒),join 邀约信息 */
    PageVO<Map<String, Object>> listMyApplications(Long userId, int page, int size);

    /** 发起人查看某邀约的申请人列表(含待审核/已通过/已拒绝),仅发起人可查 */
    java.util.List<Map<String, Object>> listParticipants(Long invitationId, Long currentUserId);

    /** 全局搜索:复用公开可见性过滤(进行中+已发布+未过期+排除拉黑),匿名不返回发起人 */
    PageVO<Map<String, Object>> searchInvitations(Long currentUserId, String keyword, int page, int size);
}
