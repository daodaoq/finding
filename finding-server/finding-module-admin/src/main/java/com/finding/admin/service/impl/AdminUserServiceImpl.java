package com.finding.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.admin.service.AdminUserService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.user.dto.UserResumeDTO;
import com.finding.user.entity.User;
import com.finding.user.entity.UserResume;
import com.finding.user.entity.UserWarning;
import com.finding.user.event.UserBannedEvent;
import com.finding.user.event.UserWarningEvent;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserWarningMapper;
import com.finding.user.service.UserResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    /** 允许的角色取值(小写存储;JWT 鉴权时统一转大写做 ROLE_ 前缀) */
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "admin");
    /** 封禁天数上限(约 10 年),防止误传超大值造成事实上永久封禁且难以察觉 */
    private static final int MAX_BAN_DAYS = 3650;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserResumeService userResumeService;
    private final ApplicationEventPublisher eventPublisher;
    private final OperationAuditService operationAuditService;
    private final UserWarningMapper userWarningMapper;

    @Override
    public PageVO<Map<String, Object>> listUsers(int page, int size, String keyword) {
        size = AdminPaging.clampSize(size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getNickname, keyword).or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("nickname", u.getNickname());
            map.put("phone", u.getPhone());
            map.put("school", u.getSchool());
            map.put("status", u.getStatus());
            map.put("realNameVerified", u.getRealNameVerified());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).toList();

        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public Map<String, Object> createUser(Map<String, Object> body) {
        User user = new User();
        user.setNickname(asString(body.get("nickname"), "新用户"));
        user.setPhone(asString(body.get("phone"), null));
        if (!StringUtils.hasText(user.getPhone())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号不能为空");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone())) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号已存在");
        }
        user.setUsername(user.getPhone());
        user.setPassword(passwordEncoder.encode(asString(body.get("password"), "123456")));
        user.setAvatar(asString(body.get("avatar"), null));
        user.setSchool(asString(body.get("school"), null));
        user.setGender(asInt(body.get("gender"), 0));
        user.setSignature(asString(body.get("signature"), null));
        user.setCity(asString(body.get("city"), null));
        user.setStatus(asInt(body.get("status"), 1));
        user.setRole(normalizeRole(asString(body.get("role"), "user")));
        user.setRealNameVerified(0);
        userMapper.insert(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        return result;
    }

    @Override
    public Map<String, Object> getUserDetail(Long id) {
        User user = requireUser(id);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("nickname", user.getNickname());
        map.put("phone", user.getPhone());
        map.put("avatar", user.getAvatar());
        map.put("school", user.getSchool());
        map.put("gender", user.getGender());
        map.put("birthday", user.getBirthday());
        map.put("email", user.getEmail());
        map.put("signature", user.getSignature());
        map.put("city", user.getCity());
        map.put("status", user.getStatus());
        map.put("role", user.getRole());
        map.put("realNameVerified", user.getRealNameVerified());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    @Override
    public void updateUser(Long id, Map<String, Object> body) {
        User user = requireUser(id);

        if (present(body, "nickname")) user.setNickname(asString(body.get("nickname"), user.getNickname()));
        if (present(body, "phone")) user.setPhone(asString(body.get("phone"), user.getPhone()));
        if (present(body, "avatar")) user.setAvatar(asString(body.get("avatar"), user.getAvatar()));
        if (present(body, "school")) user.setSchool(asString(body.get("school"), user.getSchool()));
        if (present(body, "gender")) user.setGender(asInt(body.get("gender"), user.getGender()));
        if (present(body, "birthday")) {
            String bd = body.get("birthday").toString();
            if (!bd.isEmpty()) {
                try {
                    user.setBirthday(LocalDate.parse(bd));
                } catch (Exception e) {
                    // 管理员输入不合法日期属参数错误:显式报错,不再静默丢弃
                    throw new BusinessException(ResultCode.PARAM_ERROR, "生日格式不正确，应为 yyyy-MM-dd");
                }
            }
        }
        if (present(body, "email")) user.setEmail(asString(body.get("email"), user.getEmail()));
        if (present(body, "signature")) user.setSignature(asString(body.get("signature"), user.getSignature()));
        if (present(body, "city")) user.setCity(asString(body.get("city"), user.getCity()));
        if (present(body, "status")) user.setStatus(asInt(body.get("status"), user.getStatus()));
        // 角色白名单:防止通过任意字符串提权或写入非法角色
        if (present(body, "role")) user.setRole(normalizeRole(asString(body.get("role"), user.getRole())));

        // 密码单独处理：非空才更新
        String password = asString(body.get("password"), null);
        if (StringUtils.hasText(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }

        userMapper.updateById(user);
    }

    @Override
    public void toggleUserStatus(Long id, Map<String, Object> body) {
        User user = requireUser(id);
        Integer newStatus = asInt(body.get("status"), null);
        user.setStatus(newStatus != null ? newStatus : (user.getStatus() == 1 ? 0 : 1));
        // 解封时清掉封禁到期时间与原因
        if (user.getStatus() == 1) {
            user.setBannedUntil(null);
            user.setBannedReason(null);
        }
        userMapper.updateById(user);
    }

    @Override
    public void banUser(Long adminId, Long id, Map<String, Object> body) {
        User user = requireUser(id);
        int days = asInt(body.get("days"), 0);
        if (days < 0 || days > MAX_BAN_DAYS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "封禁天数应在 0~" + MAX_BAN_DAYS + " 之间(0 表示永久)");
        }
        String reason = asString(body.get("reason"), null);
        user.setStatus(0);
        if (days > 0) {
            user.setBannedUntil(LocalDateTime.now().plusDays(days));
        } else {
            user.setBannedUntil(null); // 永久封禁
        }
        user.setBannedReason(reason);
        userMapper.updateById(user);
        // 实时通知在线用户(前端弹提示框并强制退出)
        eventPublisher.publishEvent(new UserBannedEvent(user.getId(), reason, user.getBannedUntil()));
        operationAuditService.record(adminId, "ban", "user", user.getId(),
                "封禁用户 " + (days > 0 ? days + "天" : "永久"), reason);
    }

    @Override
    public void warnUser(Long adminId, Long id, Map<String, Object> body) {
        requireUser(id);
        String raw = asString(body.get("reason"), null);
        String reason = StringUtils.hasText(raw) ? raw : "违反平台规范";
        UserWarning w = new UserWarning();
        w.setUserId(id);
        w.setReason(reason);
        w.setOperatorId(adminId);
        userWarningMapper.insert(w);
        eventPublisher.publishEvent(new UserWarningEvent(id, reason, adminId));
        operationAuditService.record(adminId, "warn", "user", id, "警告用户", reason);
    }

    @Override
    public UserResume getUserResume(Long id) {
        requireUser(id);
        return userResumeService.getMyResume(id);
    }

    @Override
    public void updateUserResume(Long id, UserResumeDTO dto) {
        requireUser(id);
        userResumeService.saveResume(id, dto);
    }

    // ── 内部工具 ──

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        return user;
    }

    /** 请求体里是否显式带了该字段且非 null(保持「缺省不修改」的既有语义) */
    private boolean present(Map<String, Object> body, String key) {
        return body.containsKey(key) && body.get(key) != null;
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    /** 数字安全转换:JSON 里传字符串 "1" 也能解析,非法值回落默认,避免 ClassCastException/NumberFormatException */
    private Integer asInt(Object value, Integer fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "参数格式不正确:" + value);
        }
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色取值不合法,仅支持 USER 或 ADMIN");
        }
        return normalized;
    }
}
