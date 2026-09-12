package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminUserLookupService;
import com.finding.admin.service.AdminVerificationService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.user.entity.User;
import com.finding.user.entity.UserVerification;
import com.finding.user.event.UserVerifiedEvent;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserVerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminVerificationServiceImpl implements AdminVerificationService {

    /** 认证状态:0=待审核 1=已通过 2=已拒绝 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    /** 用户实名状态:2=已通过 3=未通过 */
    private static final int REAL_NAME_APPROVED = 2;
    private static final int REAL_NAME_REJECTED = 3;

    private final UserVerificationMapper verificationMapper;
    private final UserMapper userMapper;
    private final AdminUserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageVO<Map<String, Object>> listVerifications(int page, int size, Integer status) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<UserVerification> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(UserVerification::getStatus, status);
        }
        wrapper.orderByDesc(UserVerification::getCreatedAt);

        Page<UserVerification> result = verificationMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, User> userMap = userLookupService.usersByIds(
                result.getRecords().stream().map(UserVerification::getUserId).toList());

        List<Map<String, Object>> records = result.getRecords().stream().map(v -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId());
            map.put("userId", v.getUserId());
            User u = userMap.get(v.getUserId());
            map.put("phone", u != null ? u.getPhone() : "");
            map.put("realName", v.getRealName());
            map.put("studentId", v.getStudentId());
            map.put("school", v.getSchool());
            map.put("idCardFront", v.getIdCardFront());
            map.put("idCardBack", v.getIdCardBack());
            map.put("studentCard", v.getStudentCard());
            map.put("status", v.getStatus());
            map.put("reviewComment", v.getReviewComment());
            map.put("createdAt", v.getCreatedAt());
            return map;
        }).toList();

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void approve(Long adminId, Long id) {
        applyReview(adminId, id, STATUS_APPROVED, REAL_NAME_APPROVED, null, true);
    }

    @Override
    @Transactional
    public void reject(Long adminId, Long id, String comment) {
        applyReview(adminId, id, STATUS_REJECTED, REAL_NAME_REJECTED, comment, false);
    }

    /** 通过与拒绝仅状态与文案不同,统一实现 */
    private void applyReview(Long adminId, Long id, int verifyStatus, int realNameStatus,
                             String comment, boolean approved) {
        UserVerification v = verificationMapper.selectById(id);
        if (v == null) throw new BusinessException(ResultCode.PARAM_ERROR, "认证记录不存在");
        if (v.getStatus() != STATUS_PENDING) throw new BusinessException(ResultCode.PARAM_ERROR, "该认证已处理");

        v.setStatus(verifyStatus);
        v.setReviewerId(adminId);           // 审核人取登录态,不接受客户端传入
        v.setReviewComment(comment);
        verificationMapper.updateById(v);

        User user = userMapper.selectById(v.getUserId());
        if (user != null) {
            user.setRealNameVerified(realNameStatus);
            if (approved) user.setStudentId(v.getStudentId());
            userMapper.updateById(user);
        }

        eventPublisher.publishEvent(new UserVerifiedEvent(v.getUserId(), approved, comment, adminId));
    }
}
