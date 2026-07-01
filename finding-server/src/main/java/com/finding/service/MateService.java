package com.finding.service;

import com.finding.dto.MateCreateDTO;
import com.finding.dto.MateQueryDTO;
import com.finding.vo.MateVO;
import com.finding.vo.PageVO;

public interface MateService {

    PageVO<MateVO> listInvitations(MateQueryDTO query, Long currentUserId);
    MateVO getInvitationDetail(Long id, Long currentUserId);
    MateVO createInvitation(Long userId, MateCreateDTO dto);
    void updateInvitation(Long userId, Long id, MateCreateDTO dto);
    void cancelInvitation(Long userId, Long id);
    void joinInvitation(Long userId, Long id, String message);
    void leaveInvitation(Long userId, Long id);
    void handleJoinRequest(Long userId, Long id, Long participantId, boolean accept);
}
