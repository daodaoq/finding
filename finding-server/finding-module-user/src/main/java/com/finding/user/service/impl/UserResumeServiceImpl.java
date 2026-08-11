package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.UserResume;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserResumeMapper;
import com.finding.user.service.InfoShareQuery;
import com.finding.user.service.UserRelationshipService;
import com.finding.user.service.UserResumeService;
import com.finding.user.vo.ResumeViewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserResumeServiceImpl implements UserResumeService {

    private final UserResumeMapper resumeMapper;
    private final UserMapper userMapper;
    private final InfoShareQuery infoShareQuery;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final UserRelationshipService relationshipService;

    @Override
    public UserResume getMyResume(Long userId) {
        return selectByUserId(userId);
    }

    @Override
    @Transactional
    public void saveResume(Long userId, UserResumeDTO dto) {
        // 简历任一文本字段含违禁词直接拒绝保存
        sensitiveWordFilter.assertClean(resumeTexts(dto));
        // 生日为年龄唯一来源:有生日则覆盖 age,避免与生日不一致
        if (dto.getBirthday() != null) {
            dto.setAge(Period.between(dto.getBirthday(), LocalDate.now()).getYears());
        }
        // 生活相册数量与 URL 校验
        validatePhotoAlbum(dto.getPhotoAlbum());

        UserResume resume = selectByUserId(userId);
        if (resume == null) {
            resume = new UserResume();
            resume.setUserId(userId);
            BeanUtils.copyProperties(dto, resume);
            try {
                resumeMapper.insert(resume);
            } catch (DuplicateKeyException e) {
                // 并发首存冲突(user_resume.user_id 唯一约束):另一请求已创建,回查后更新
                resume = selectByUserId(userId);
                if (resume != null) {
                    BeanUtils.copyProperties(dto, resume, "id", "userId", "createdAt", "updatedAt");
                    resumeMapper.updateById(resume);
                }
            }
        } else {
            BeanUtils.copyProperties(dto, resume, "id", "userId", "createdAt", "updatedAt");
            resumeMapper.updateById(resume);
        }
    }

    /** 生活相册校验:数量上限 + 每张 URL 仅允许 http(s) 且长度受限 */
    private void validatePhotoAlbum(List<String> album) {
        if (album == null) return;
        if (album.size() > 9) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "相册最多 9 张");
        }
        for (String url : album) {
            if (url == null || url.isBlank()) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "相册图片 URL 不能为空");
            }
            if (url.length() > 1000) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "相册图片 URL 过长");
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "相册图片 URL 仅支持 http(s)");
            }
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
        // 拉黑后即使已互换也不再返回详细资料(限制已互换资料可见)
        boolean shared = status == InfoShareQuery.STATUS_APPROVED
                && !relationshipService.isBlockedEitherWay(currentUserId, targetUserId);
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

    /** 收集 DTO 中所有非空 String 字段值,供违禁词统一校验 */
    private List<String> resumeTexts(UserResumeDTO dto) {
        List<String> texts = new ArrayList<>();
        for (java.lang.reflect.Field f : dto.getClass().getDeclaredFields()) {
            if (f.getType() == String.class) {
                try {
                    f.setAccessible(true);
                    String v = (String) f.get(dto);
                    if (v != null && !v.isEmpty()) texts.add(v);
                } catch (IllegalAccessException e) {
                    // 反射失败不影响发布(理论不会发生),debug 记录便于排查
                    log.debug("简历文本反射收集失败: field={}, cause={}", f.getName(), e.getMessage());
                }
            }
        }
        return texts;
    }
}
