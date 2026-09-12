package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.user.entity.UserRemark;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserRemarkMapper;
import com.finding.user.service.RemarkCache;
import com.finding.user.service.UserRemarkService;
import com.finding.user.service.UserWriteGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRemarkServiceImpl implements UserRemarkService {

    /** 与昵称输入、@提及正则一致的长度上限 */
    private static final int MAX_LEN = 20;

    private final UserRemarkMapper userRemarkMapper;
    private final UserMapper userMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final UserWriteGuard userWriteGuard;
    private final RemarkCache remarkCache;

    @Override
    @Transactional
    public void setRemark(Long userId, Long targetUserId, String remark) {
        if (userId == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        if (targetUserId == null) throw new BusinessException(ResultCode.PARAM_ERROR, "参数错误");
        if (userId.equals(targetUserId)) throw new BusinessException(ResultCode.PARAM_ERROR, "不能给自己设置备注");
        if (userMapper.selectById(targetUserId) == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        userWriteGuard.checkWritable(userId);

        // 顺序:清洗 → 长度 → 违禁词 → 落库(落库的是清洗后的值)
        String clean = XssUtil.clean(remark == null ? "" : remark.trim());
        if (clean == null || clean.isEmpty()) throw new BusinessException(ResultCode.PARAM_ERROR, "备注不能为空");
        if (clean.length() > MAX_LEN) throw new BusinessException(ResultCode.PARAM_ERROR, "备注最长 " + MAX_LEN + " 个字");
        sensitiveWordFilter.assertClean(clean);

        userRemarkMapper.upsert(userId, targetUserId, clean);
        remarkCache.evict(userId, targetUserId);
    }

    @Override
    @Transactional
    public void clearRemark(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) return;
        userRemarkMapper.delete(new LambdaQueryWrapper<UserRemark>()
                .eq(UserRemark::getUserId, userId)
                .eq(UserRemark::getTargetUserId, targetUserId));
        remarkCache.evict(userId, targetUserId);
    }

    @Override
    public String getRemark(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) return null;
        UserRemark row = userRemarkMapper.selectOne(new LambdaQueryWrapper<UserRemark>()
                .eq(UserRemark::getUserId, userId)
                .eq(UserRemark::getTargetUserId, targetUserId));
        return row == null ? null : row.getRemark();
    }
}
