package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.constant.UserStatusEnum;
import com.finding.common.event.AccountDeletedEvent;
import com.finding.user.dto.LoginDTO;
import com.finding.user.dto.RegisterDTO;
import com.finding.user.entity.User;
import com.finding.user.entity.UserFollow;
import com.finding.user.entity.UserVerification;
import com.finding.user.mapper.UserFollowMapper;
import com.finding.user.mapper.UserMapper;
import com.finding.user.mapper.UserVerificationMapper;
import com.finding.user.security.JwtTokenProvider;
import com.finding.user.service.AuthService;
import com.finding.user.service.UserPostStatsQuery;
import com.finding.common.RedisUtils;
import com.finding.common.util.XssUtil;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.user.util.CaptchaGenerator;
import com.finding.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserVerificationMapper verificationMapper;
    private final UserFollowMapper followMapper;
    private final UserPostStatsQuery userPostStatsQuery;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtils redisUtils;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_LIMIT_PREFIX = "sms:limit:";
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_PREFIX = "token:refresh:";
    private static final String LOGIN_FAIL_PHONE_PREFIX = "login:fail:phone:";
    private static final String LOGIN_FAIL_IP_PREFIX = "login:fail:ip:";
    private static final String SMS_SEND_IP_PREFIX = "sms:send-ip:";
    private static final String SMS_VERIFY_FAIL_PREFIX = "sms:verify-fail:";

    /** 登录失败锁定:同账号/IP 达到上限后 15 分钟内禁止登录 */
    private static final int LOGIN_FAIL_LIMIT = 5;
    private static final long LOGIN_LOCK_MINUTES = 15;
    /** 短信验证码错误次数上限(超过需重新获取) */
    private static final int SMS_VERIFY_FAIL_LIMIT = 5;
    /** 同 IP 每小时发送验证码上限 */
    private static final int SMS_SEND_IP_LIMIT = 10;

    /** 防批量注册:同设备每小时最多注册次数 */
    @Value("${finding.register.device-limit:3}")
    private int registerDeviceLimit = 3;

    /** 防批量注册:同 IP 每小时最多注册次数 */
    @Value("${finding.register.ip-limit:10}")
    private int registerIpLimit = 10;

    @Override
    public Map<String, String> login(LoginDTO dto, String ip) {
        // 登录失败锁定:账号或 IP 任一达到上限即拒绝
        checkLoginLocked(dto.getPhone(), ip);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            recordLoginFailure(dto.getPhone(), ip);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() != UserStatusEnum.ACTIVE.getCode()) {
            // 定时封禁到期 → 自动解封
            if (user.getStatus() == UserStatusEnum.BANNED.getCode()
                    && user.getBannedUntil() != null
                    && user.getBannedUntil().isBefore(LocalDateTime.now())) {
                user.setStatus(UserStatusEnum.ACTIVE.getCode());
                user.setBannedUntil(null);
                userMapper.updateById(user);
            } else {
                throw new BusinessException(ResultCode.ACCOUNT_DISABLED, banMessage(user));
            }
        }

        try {
            if ("password".equals(dto.getLoginType())) {
                if (!StringUtils.hasText(dto.getPassword())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "密码不能为空");
                }
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(dto.getPhone(), dto.getPassword()));
            } else if ("sms".equals(dto.getLoginType())) {
                if (!StringUtils.hasText(dto.getSmsCode())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "验证码不能为空");
                }
                verifySmsCode(dto.getPhone(), dto.getSmsCode());
            } else {
                throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的登录类型");
            }
        } catch (BadCredentialsException e) {
            recordLoginFailure(dto.getPhone(), ip);
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 登录成功,清空失败计数
        clearLoginFailure(dto.getPhone(), ip);

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())));
        String accessToken = jwtTokenProvider.createAccessToken(auth);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        redisUtils.set(REFRESH_PREFIX + user.getId(), refreshToken, 7, TimeUnit.DAYS);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        return result;
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto, String ip, String deviceId) {
        // 防批量注册:已达上限直接拒绝(仅在注册成功后计数)
        checkRegisterFlood(ip, deviceId);
        // 图片验证码校验(一次性,校验后删除)
        verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());

        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())) > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getPhone());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setSchool(dto.getSchool());
        user.setGender(dto.getGender() != null ? dto.getGender() : 0);
        user.setRole("user");
        user.setStatus(UserStatusEnum.ACTIVE.getCode());
        // XSS 清洗 + 违禁词拦截
        dto.setNickname(XssUtil.clean(dto.getNickname()));
        dto.setSchool(XssUtil.clean(dto.getSchool()));
        sensitiveWordFilter.assertClean(dto.getNickname(), dto.getSchool());
        userMapper.insert(user);

        // 注册成功后再计数,失败尝试不计(避免卡死正常用户)
        recordRegister(ip, deviceId);
        log.info("新用户注册: id={}, phone={}", user.getId(), dto.getPhone());
    }

    /** 防批量注册:同设备/同 IP 每小时注册次数上限(只读检查,不计数) */
    private void checkRegisterFlood(String ip, String deviceId) {
        if (StringUtils.hasText(deviceId) && registerCount("register:device:" + deviceId) >= registerDeviceLimit) {
            throw new BusinessException(ResultCode.TOO_FREQUENT, "注册过于频繁，请稍后再试");
        }
        if (StringUtils.hasText(ip) && registerCount("register:ip:" + ip) >= registerIpLimit) {
            throw new BusinessException(ResultCode.TOO_FREQUENT, "注册过于频繁，请稍后再试");
        }
    }

    /** 注册成功后计数(带过期) */
    private void recordRegister(String ip, String deviceId) {
        if (StringUtils.hasText(deviceId)) {
            String key = "register:device:" + deviceId;
            long c = redisUtils.increment(key, 1);
            if (c == 1) redisUtils.expire(key, 1, TimeUnit.HOURS);
        }
        if (StringUtils.hasText(ip)) {
            String key = "register:ip:" + ip;
            long c = redisUtils.increment(key, 1);
            if (c == 1) redisUtils.expire(key, 1, TimeUnit.HOURS);
        }
    }

    /** 读取 Redis 计数(容错:任何序列化形态/异常都按 0 处理) */
    private long readLong(String key) {
        Object v = redisUtils.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v != null) {
            try {
                return Long.parseLong(v.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    /** 计数 +1(首次写入时设置过期窗口) */
    private void incrExpiring(String key, long minutes) {
        long c = redisUtils.increment(key, 1);
        if (c == 1) redisUtils.expire(key, minutes, TimeUnit.MINUTES);
    }

    /** 读取注册计数 */
    private long registerCount(String key) {
        return readLong(key);
    }

    /** 检查登录失败锁定:账号或 IP 任一达到上限即拒绝 */
    private void checkLoginLocked(String phone, String ip) {
        if (readLong(LOGIN_FAIL_PHONE_PREFIX + phone) >= LOGIN_FAIL_LIMIT
                || (StringUtils.hasText(ip) && readLong(LOGIN_FAIL_IP_PREFIX + ip) >= LOGIN_FAIL_LIMIT)) {
            throw new BusinessException(ResultCode.TOO_FREQUENT, "登录失败次数过多，请稍后再试");
        }
    }

    /** 记录一次登录失败(账号 + IP 双维度,带过期窗口) */
    private void recordLoginFailure(String phone, String ip) {
        incrExpiring(LOGIN_FAIL_PHONE_PREFIX + phone, LOGIN_LOCK_MINUTES);
        if (StringUtils.hasText(ip)) incrExpiring(LOGIN_FAIL_IP_PREFIX + ip, LOGIN_LOCK_MINUTES);
    }

    /** 登录成功后清空失败计数 */
    private void clearLoginFailure(String phone, String ip) {
        redisUtils.delete(LOGIN_FAIL_PHONE_PREFIX + phone);
        if (StringUtils.hasText(ip)) redisUtils.delete(LOGIN_FAIL_IP_PREFIX + ip);
    }

    @Override
    public void sendCode(String phone, String type, String ip) {
        String limitKey = SMS_LIMIT_PREFIX + phone;
        if (redisUtils.exists(limitKey)) {
            throw new BusinessException(ResultCode.SMS_SEND_TOO_FREQUENT);
        }
        // 同 IP 每小时发送上限,防止短信轰炸
        if (StringUtils.hasText(ip) && readLong(SMS_SEND_IP_PREFIX + ip) >= SMS_SEND_IP_LIMIT) {
            throw new BusinessException(ResultCode.TOO_FREQUENT, "发送验证码过于频繁，请稍后再试");
        }

        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        redisUtils.set(SMS_CODE_PREFIX + type + ":" + phone, code, 5, TimeUnit.MINUTES);
        redisUtils.set(limitKey, "1", 60, TimeUnit.SECONDS);
        if (StringUtils.hasText(ip)) {
            incrExpiring(SMS_SEND_IP_PREFIX + ip, 60);
        }

        log.info("SMS验证码已发送 phone={} type={}", phone, type);
    }

    @Override
    public Map<String, String> generateCaptcha() {
        try {
            String key = UUID.randomUUID().toString().replace("-", "");
            String code = CaptchaGenerator.randomCode(4);
            redisUtils.set(CAPTCHA_PREFIX + key, code, 5, TimeUnit.MINUTES);

            String image = CaptchaGenerator.drawImage(code);
            Map<String, String> result = new HashMap<>();
            result.put("captchaKey", key);
            result.put("captchaImage", image);
            return result;
        } catch (Exception e) {
            log.error("生成图片验证码失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "验证码生成失败，请重试");
        }
    }

    @Override
    public String refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String stored = redisUtils.get(REFRESH_PREFIX + userId);
        if (!refreshToken.equals(stored)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != UserStatusEnum.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())));
        return jwtTokenProvider.createAccessToken(auth);
    }

    @Override
    public void logout(String accessToken) {
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        long remainingMs = jwtTokenProvider.getAccessExpiration();
        if (remainingMs > 0) {
            redisUtils.set(TOKEN_BLACKLIST_PREFIX + accessToken, "1", remainingMs, TimeUnit.MILLISECONDS);
        }
        if (userId != null) {
            redisUtils.delete(REFRESH_PREFIX + userId);
        }
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        UserVO vo = toVO(user);
        // 生日仅本人可见(用于卡片预览计算年龄)
        vo.setBirthday(user.getBirthday());

        // 统计关注/粉丝/动态数
        vo.setFollowerCount(followMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFolloweeId, userId)).intValue());
        vo.setFollowingCount(followMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId)).intValue());
        vo.setPostCount(userPostStatsQuery.countPosts(userId));
        // 互关(好友)数:关注我的人中,我也关注了他们
        vo.setMutualCount(followMapper.selectCount(
                        new LambdaQueryWrapper<UserFollow>()
                                .eq(UserFollow::getFolloweeId, userId)
                                .inSql(UserFollow::getFollowerId,
                                        "SELECT followee_id FROM user_follow WHERE follower_id = " + userId))
                .intValue());

        return vo;
    }

    @Override
    public void updateProfile(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (StringUtils.hasText(vo.getNickname())) user.setNickname(vo.getNickname());
        if (vo.getAvatar() != null) user.setAvatar(vo.getAvatar());
        if (vo.getProfileBackground() != null) {
            if (vo.getProfileBackground().length() > 500) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "profileBackground 长度不能超过 500");
            }
            user.setProfileBackground(vo.getProfileBackground());
        }
        if (vo.getSignature() != null) user.setSignature(vo.getSignature());
        if (vo.getSchool() != null) user.setSchool(vo.getSchool());
        if (vo.getGender() != null) {
            if (vo.getGender() < 0 || vo.getGender() > 2) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "gender 仅允许 0/1/2");
            }
            user.setGender(vo.getGender());
        }
        if (vo.getTargetType() != null) {
            if (vo.getTargetType() < 0 || vo.getTargetType() > 2) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "targetType 仅允许 0/1/2");
            }
            user.setTargetType(vo.getTargetType());
        }
        if (vo.getCity() != null) user.setCity(vo.getCity());
        // XSS 清洗 + 违禁词拦截
        vo.setNickname(XssUtil.clean(vo.getNickname()));
        vo.setSignature(XssUtil.clean(vo.getSignature()));
        vo.setSchool(XssUtil.clean(vo.getSchool()));
        vo.setCity(XssUtil.clean(vo.getCity()));
        sensitiveWordFilter.assertClean(vo.getNickname(), vo.getSignature(), vo.getSchool(), vo.getCity());
        userMapper.updateById(user);
    }

    @Override
    public void submitVerification(Long userId, String realName, String studentId, String school,
                                   String idCardFront, String idCardBack, String studentCard) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (user.getRealNameVerified() == 1) throw new BusinessException(ResultCode.VERIFICATION_PENDING);
        if (user.getRealNameVerified() == 2) throw new BusinessException(ResultCode.PARAM_ERROR, "已完成实名认证");

        UserVerification verification = new UserVerification();
        verification.setUserId(userId);
        verification.setRealName(realName);
        verification.setStudentId(studentId);
        verification.setSchool(school);
        verification.setIdCardFront(idCardFront);
        verification.setIdCardBack(idCardBack);
        verification.setStudentCard(studentCard);
        verification.setStatus(0);
        verificationMapper.insert(verification);

        user.setRealNameVerified(1);
        userMapper.updateById(user);
    }

    @Override
    public UserVerification getMyVerification(Long userId) {
        // 只按当前用户ID查，天然只能看到自己的认证记录
        UserVerification v = verificationMapper.selectOne(
                new LambdaQueryWrapper<UserVerification>()
                        .eq(UserVerification::getUserId, userId)
                        .orderByDesc(UserVerification::getCreatedAt)
                        .last("LIMIT 1"));
        // 老数据/种子账号:user 表已认证但没有认证记录时,从 user 实体兜底拼一份
        if (v == null) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getRealNameVerified() != null && user.getRealNameVerified() == 2) {
                v = new UserVerification();
                v.setUserId(userId);
                v.setRealName(""); // user 表未存真实姓名
                v.setStudentId(user.getStudentId());
                v.setSchool(user.getSchool());
                v.setStatus(1); // 已通过
            }
        }
        return v;
    }

    @Override
    public Map<String, String> getAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        Map<String, String> map = new HashMap<>();
        map.put("phone", user.getPhone() != null ? user.getPhone() : "");
        return map;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (!StringUtils.hasText(oldPassword) || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "旧密码不正确");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码至少 6 位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        // 作废旧 refresh token,强制其他端重新登录
        redisUtils.delete(REFRESH_PREFIX + userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码不正确");
        }
        // 匿名化资料:手机号/用户名改为随机值(原手机号无法再登录),昵称改为占位,清空个人字段
        String anon = "del_" + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        user.setUsername(anon);
        user.setPhone(anon);
        user.setPassword(UUID.randomUUID().toString()); // 密码置随机,原密码失效
        user.setNickname("已注销用户");
        user.setAvatar(null);
        user.setProfileBackground(null);
        user.setGender(0);
        user.setBirthday(null);
        user.setSchool(null);
        user.setStudentId(null);
        user.setSignature(null);
        user.setCity(null);
        user.setLatitude(null);
        user.setLongitude(null);
        user.setEmail(null);
        user.setRealNameVerified(0);
        user.setTargetType(0);
        user.setStatus(UserStatusEnum.DELETED.getCode());
        userMapper.updateById(user);
        // 撤销刷新令牌(访问令牌由 JWT 过滤器按 status!=1 即时失效)
        redisUtils.delete(REFRESH_PREFIX + userId);
        // 联动:取消涉及该用户的待处理聊天申请/信息互换(chat 模块监听)
        eventPublisher.publishEvent(new AccountDeletedEvent(userId));
        log.info("账号已注销: userId={}", userId);
    }

    // ── 私有方法 ──

    /** 封禁/冻结提示文案(含原因) */
    private String banMessage(User user) {
        if (user.getStatus() != null && user.getStatus() == UserStatusEnum.FROZEN.getCode()) {
            return "该账号已被冻结";
        }
        String reason = user.getBannedReason();
        if (user.getBannedUntil() != null) {
            String until = user.getBannedUntil()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return (reason != null && !reason.isEmpty())
                    ? "该账号已被封禁至 " + until + "，原因：" + reason
                    : "该账号已被封禁至 " + until;
        }
        return (reason != null && !reason.isEmpty())
                ? "该账号已被永久封禁，原因：" + reason
                : "该账号已被封禁";
    }

    /** 校验图片验证码(一次性,校验后删除) */
    private void verifyCaptcha(String key, String code) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请输入图片验证码");
        }
        String stored = redisUtils.get(CAPTCHA_PREFIX + key);
        if (stored == null) {
            throw new BusinessException(ResultCode.SMS_CODE_EXPIRED, "验证码已过期，请刷新");
        }
        if (!stored.equalsIgnoreCase(code)) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR, "验证码错误");
        }
        redisUtils.delete(CAPTCHA_PREFIX + key);
    }

    private void verifySmsCode(String phone, String code) {
        // 错误次数上限,防止 6 位验证码被暴力枚举
        if (readLong(SMS_VERIFY_FAIL_PREFIX + phone) >= SMS_VERIFY_FAIL_LIMIT) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR, "验证码错误次数过多，请重新获取验证码");
        }
        String stored = redisUtils.get(SMS_CODE_PREFIX + "login:" + phone);
        if (stored == null) stored = redisUtils.get(SMS_CODE_PREFIX + "register:" + phone);
        if (stored == null) throw new BusinessException(ResultCode.SMS_CODE_EXPIRED);
        if (!stored.equals(code)) {
            incrExpiring(SMS_VERIFY_FAIL_PREFIX + phone, 5);
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        // 校验通过:清空错误计数并删除验证码(一次性)
        redisUtils.delete(SMS_VERIFY_FAIL_PREFIX + phone);
        redisUtils.delete(SMS_CODE_PREFIX + "login:" + phone);
        redisUtils.delete(SMS_CODE_PREFIX + "register:" + phone);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setProfileBackground(user.getProfileBackground());
        vo.setGender(user.getGender());
        vo.setSchool(user.getSchool());
        vo.setSignature(user.getSignature());
        vo.setCity(user.getCity());
        vo.setRealNameVerified(user.getRealNameVerified());
        vo.setTargetType(user.getTargetType());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
