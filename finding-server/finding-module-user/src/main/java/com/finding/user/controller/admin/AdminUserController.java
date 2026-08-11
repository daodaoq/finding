package com.finding.user.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.Result;
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
import com.finding.user.security.JwtInterceptor;
import com.finding.user.service.UserResumeService;
import com.finding.common.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员 - 用户管理。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserResumeService userResumeService;
    private final ApplicationEventPublisher eventPublisher;
    private final OperationAuditService operationAuditService;
    private final UserWarningMapper userWarningMapper;

    @GetMapping("/users")
    public Result<PageVO<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

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

        return Result.ok(PageVO.of(records, result.getTotal(), page, size));
    }

    /** 新建用户 */
    @PostMapping("/users")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        User user = new User();
        user.setNickname((String) body.getOrDefault("nickname", "新用户"));
        user.setPhone((String) body.get("phone"));
        if (!StringUtils.hasText(user.getPhone())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号不能为空");
        }
        // 检查手机号唯一
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone())) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "手机号已存在");
        }
        user.setUsername(user.getPhone());
        user.setPassword(passwordEncoder.encode(
                (String) body.getOrDefault("password", "123456")));
        user.setAvatar((String) body.get("avatar"));
        user.setSchool((String) body.get("school"));
        user.setGender(body.get("gender") != null ? (Integer) body.get("gender") : 0);
        user.setSignature((String) body.get("signature"));
        user.setCity((String) body.get("city"));
        user.setStatus(body.get("status") != null ? (Integer) body.get("status") : 1);
        user.setRole((String) body.getOrDefault("role", "user"));
        user.setRealNameVerified(0);
        userMapper.insert(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        return Result.ok(result);
    }

    /** 获取用户详情（含完整字段） */
    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");

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
        return Result.ok(map);
    }

    /** 编辑用户信息 */
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");

        if (body.containsKey("nickname") && body.get("nickname") != null)
            user.setNickname((String) body.get("nickname"));
        if (body.containsKey("phone") && body.get("phone") != null)
            user.setPhone((String) body.get("phone"));
        if (body.containsKey("avatar") && body.get("avatar") != null)
            user.setAvatar((String) body.get("avatar"));
        if (body.containsKey("school") && body.get("school") != null)
            user.setSchool((String) body.get("school"));
        if (body.containsKey("gender") && body.get("gender") != null)
            user.setGender((Integer) body.get("gender"));
        if (body.containsKey("birthday") && body.get("birthday") != null) {
            String bd = body.get("birthday").toString();
            if (!bd.isEmpty()) {
                try {
                    user.setBirthday(java.time.LocalDate.parse(bd));
                } catch (Exception e) {
                    // 管理员输入不合法日期属参数错误:显式报错,不再静默丢弃
                    throw new BusinessException(ResultCode.PARAM_ERROR, "生日格式不正确，应为 yyyy-MM-dd");
                }
            }
        }
        if (body.containsKey("email") && body.get("email") != null)
            user.setEmail((String) body.get("email"));
        if (body.containsKey("signature") && body.get("signature") != null)
            user.setSignature((String) body.get("signature"));
        if (body.containsKey("city") && body.get("city") != null)
            user.setCity((String) body.get("city"));
        if (body.containsKey("status") && body.get("status") != null)
            user.setStatus((Integer) body.get("status"));
        if (body.containsKey("role") && body.get("role") != null)
            user.setRole((String) body.get("role"));

        // 密码单独处理：非空才更新
        String password = (String) body.get("password");
        if (StringUtils.hasText(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }

        userMapper.updateById(user);
        return Result.ok();
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        Integer newStatus = body.get("status");
        user.setStatus(newStatus != null ? newStatus : (user.getStatus() == 1 ? 0 : 1));
        // 解封时清掉封禁到期时间与原因
        if (user.getStatus() == 1) {
            user.setBannedUntil(null);
            user.setBannedReason(null);
        }
        userMapper.updateById(user);
        return Result.ok();
    }

    /** 封禁用户(支持按天 + 原因):days>0 封禁 days 天,days=0 永久;发布事件让在线用户实时收到提示 */
    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        Object daysRaw = body.get("days");
        int days = daysRaw != null ? ((Number) daysRaw).intValue() : 0;
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
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
        operationAuditService.record(JwtInterceptor.getCurrentUserId(), "ban", "user", user.getId(),
                "封禁用户 " + (days > 0 ? days + "天" : "永久"), reason);
        return Result.ok();
    }

    /** 警告用户一次:记录 + 站内通知 */
    @PutMapping("/users/{id}/warn")
    public Result<Void> warnUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        String reason = body.get("reason") != null && !body.get("reason").toString().isBlank()
                ? body.get("reason").toString() : "违反平台规范";
        Long operator = JwtInterceptor.getCurrentUserId();
        UserWarning w = new UserWarning();
        w.setUserId(id);
        w.setReason(reason);
        w.setOperatorId(operator);
        userWarningMapper.insert(w);
        eventPublisher.publishEvent(new UserWarningEvent(id, reason, operator));
        operationAuditService.record(operator, "warn", "user", id, "警告用户", reason);
        return Result.ok();
    }

    /** 查看任意用户的情感简历(绕过互换权限，仅管理员可见) */
    @GetMapping("/users/{id}/resume")
    public Result<UserResume> getUserResume(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        return Result.ok(userResumeService.getMyResume(id));
    }

    /** 编辑任意用户的情感简历(管理员可改一切) */
    @PutMapping("/users/{id}/resume")
    public Result<Void> updateUserResume(@PathVariable Long id, @RequestBody UserResumeDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        userResumeService.saveResume(id, dto);
        return Result.ok();
    }
}
