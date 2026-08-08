package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserResumeMapper;
import com.finding.user.service.InfoShareQuery;
import com.finding.user.service.UserResumeService;
import com.finding.user.vo.ResumeViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserResumeServiceImpl implements UserResumeService {

    private final UserResumeMapper resumeMapper;
    private final UserMapper userMapper;
    private final InfoShareQuery infoShareQuery;

    @Override
    public UserResume getMyResume(Long userId) {
        return selectByUserId(userId);
    }

    @Override
    @Transactional
    public void saveResume(Long userId, UserResumeDTO dto) {
        UserResume resume = selectByUserId(userId);
        if (resume == null) {
            resume = new UserResume();
            resume.setUserId(userId);
            BeanUtils.copyProperties(dto, resume);
            resumeMapper.insert(resume);
        } else {
            BeanUtils.copyProperties(dto, resume, "id", "userId", "createdAt", "updatedAt");
            resumeMapper.updateById(resume);
        }
    }

    @Override
    public ResumeViewVO getResumeForView(Long currentUserId, Long targetUserId) {
        if (userMapper.selectById(targetUserId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        ResumeViewVO vo = new ResumeViewVO();
        // currentUserId 为 null 表示未登录,按未互换处理
        int status = currentUserId == null ? InfoShareQuery.STATUS_NONE
                : infoShareQuery.getShareStatus(currentUserId, targetUserId);
        vo.setShareStatus(status);
        boolean shared = status == InfoShareQuery.STATUS_APPROVED;
        vo.setInfoShared(shared);
        if (shared) {
            vo.setResume(selectByUserId(targetUserId));
        }
        return vo;
    }

    private UserResume selectByUserId(Long userId) {
        return resumeMapper.selectOne(new LambdaQueryWrapper<UserResume>()
                .eq(UserResume::getUserId, userId));
    }
}
