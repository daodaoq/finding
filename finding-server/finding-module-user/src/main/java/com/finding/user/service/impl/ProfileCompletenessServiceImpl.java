package com.finding.user.service.impl;

import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.user.entity.User;
import com.finding.user.mapper.UserMapper;
import com.finding.user.service.ProfileCompletenessService;
import com.finding.user.vo.ProfileCompletenessVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 资料完整度(0-10)与缺失项。
 * 评分口径与 BridgeServiceImpl.completeness 保持一致(头像/学校/城市/性别/个性签名/生日 6 项)。
 */
@Service
@RequiredArgsConstructor
public class ProfileCompletenessServiceImpl implements ProfileCompletenessService {

    private static final int TOTAL = 6;

    private final UserMapper userMapper;

    @Override
    public ProfileCompletenessVO completeness(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(u.getAvatar())) missing.add("头像");
        if (!StringUtils.hasText(u.getSchool())) missing.add("学校");
        if (!StringUtils.hasText(u.getCity())) missing.add("城市");
        if (u.getGender() == null || u.getGender() <= 0) missing.add("性别");
        if (!StringUtils.hasText(u.getSignature())) missing.add("个性签名");
        if (u.getBirthday() == null) missing.add("生日");

        int filled = TOTAL - missing.size();
        ProfileCompletenessVO vo = new ProfileCompletenessVO();
        vo.setScore(filled * 10 / TOTAL);
        vo.setFilled(filled);
        vo.setTotal(TOTAL);
        vo.setMissing(missing);
        return vo;
    }
}
