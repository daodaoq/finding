package com.finding.user.service;

import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;
import com.finding.user.vo.ResumeViewVO;

public interface UserResumeService {

    /**
     * 「已开启情感简历」的用户 id 子查询,供各模块把匹配资格的过滤下推到 SQL,
     * 避免相识/推荐/心动各自重复实现同一条件。
     */
    String ENABLED_USER_IDS_SQL = "SELECT user_id FROM user_resume WHERE enabled = 1";

    /** 获取自己的情感简历(未填写返回 null) */
    UserResume getMyResume(Long userId);

    /** 保存/更新自己的情感简历 */
    void saveResume(Long userId, UserResumeDTO dto);

    /** 查看他人情感简历(需已互换信息,否则返回锁定状态) */
    ResumeViewVO getResumeForView(Long currentUserId, Long targetUserId);

    /** 该用户是否已开启情感简历(未填写记录或开关为 0 均返回 false) */
    boolean isResumeEnabled(Long userId);
}
