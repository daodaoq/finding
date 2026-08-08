package com.finding.user.service;

import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;
import com.finding.user.vo.ResumeViewVO;

public interface UserResumeService {

    /** 获取自己的情感简历(未填写返回 null) */
    UserResume getMyResume(Long userId);

    /** 保存/更新自己的情感简历 */
    void saveResume(Long userId, UserResumeDTO dto);

    /** 查看他人情感简历(需已互换信息,否则返回锁定状态) */
    ResumeViewVO getResumeForView(Long currentUserId, Long targetUserId);
}
