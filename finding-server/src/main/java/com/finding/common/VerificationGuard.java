package com.finding.common;

import com.finding.entity.User;
import com.finding.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 认证状态守卫 —— 检查用户是否已完成学生认证。
 * 未认证用户（realNameVerified != 2）无法使用发帖、评论、创建搭子、加入搭子、私信等功能。
 */
@Component
@RequiredArgsConstructor
public class VerificationGuard {

    private final UserMapper userMapper;

    /**
     * 检查用户是否已通过认证，未通过则抛出 BusinessException。
     *
     * @param userId 当前用户 ID
     * @throws BusinessException 如果用户未通过认证
     */
    public void checkVerified(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        switch (user.getRealNameVerified()) {
            case 0:
                throw new BusinessException(ResultCode.VERIFICATION_REQUIRED);
            case 1:
                throw new BusinessException(ResultCode.VERIFICATION_PENDING);
            case 3:
                throw new BusinessException(ResultCode.VERIFICATION_REJECTED);
            case 2:
                // 已认证，放行
                break;
            default:
                throw new BusinessException(ResultCode.VERIFICATION_REQUIRED);
        }
    }

    /**
     * 获取用户的认证状态描述，供前端展示提示文案。
     */
    public VerificationStatus getStatus(Long userId) {
        if (userId == null) return VerificationStatus.NOT_LOGGED_IN;
        User user = userMapper.selectById(userId);
        if (user == null) return VerificationStatus.NOT_LOGGED_IN;
        return switch (user.getRealNameVerified()) {
            case 0 -> VerificationStatus.UNVERIFIED;
            case 1 -> VerificationStatus.PENDING;
            case 2 -> VerificationStatus.VERIFIED;
            case 3 -> VerificationStatus.REJECTED;
            default -> VerificationStatus.UNVERIFIED;
        };
    }

    public enum VerificationStatus {
        NOT_LOGGED_IN,
        UNVERIFIED,
        PENDING,
        VERIFIED,
        REJECTED
    }
}
