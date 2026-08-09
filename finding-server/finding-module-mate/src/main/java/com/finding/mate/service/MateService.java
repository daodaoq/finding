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
    void joinInvitation(Long userId, Long id, String message);
    void leaveInvitation(Long userId, Long id);
    void handleJoinRequest(Long userId, Long id, Long participantId, boolean accept);

    PageVO<MateVO> getMyInvitations(Long userId, MateQueryDTO query);
    PageVO<MateVO> getMyJoinedInvitations(Long userId, MateQueryDTO query);

    /** 我的全部申请记录(含待审核/已通过/被拒),join 邀约信息 */
    PageVO<Map<String, Object>> listMyApplications(Long userId, int page, int size);
}
