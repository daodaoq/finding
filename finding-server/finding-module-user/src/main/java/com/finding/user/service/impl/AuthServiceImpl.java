package com.finding.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.constant.UserStatusEnum;
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
import com.finding.user.util.CaptchaGenerator;
import com.finding.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_LIMIT_PREFIX = "sms:limit:";
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_PREFIX = "token:refresh:";

    @Override
    public Map<String, String> login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
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

        if ("password".equals(dto.getLoginType())) {
            if (!StringUtils.hasText(dto.getPassword())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "密码不能为空");
            }
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(dto.getPhone(), dto.getPassword()));
            } catch (BadCredentialsException e) {
                throw new BusinessException(ResultCode.LOGIN_FAILED);
            }
        } else if ("sms".equals(dto.getLoginType())) {
            if (!StringUtils.hasText(dto.getSmsCode())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "验证码不能为空");
            }
            verifySmsCode(dto.getPhone(), dto.getSmsCode());
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的登录类型");
        }

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
    public void register(RegisterDTO dto) {
        // 图片验证码校验(替代短信验证码)
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
        userMapper.insert(user);

        log.info("新用户注册: id={}, phone={}", user.getId(), dto.getPhone());
    }

    @Override
    public void sendCode(String phone, String type) {
        String limitKey = SMS_LIMIT_PREFIX + phone;
        if (redisUtils.exists(limitKey)) {
            throw new BusinessException(ResultCode.SMS_SEND_TOO_FREQUENT);
        }

        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        redisUtils.set(SMS_CODE_PREFIX + type + ":" + phone, code, 5, TimeUnit.MINUTES);
        redisUtils.set(limitKey, "1", 60, TimeUnit.SECONDS);

        log.info("SMS验证码 phone={} type={} code={}", phone, type, code);
    }

    @Override
    public Map<String, String> generateCaptcha() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String code = CaptchaGenerator.randomCode(4);
        redisUtils.set(CAPTCHA_PREFIX + key, code, 5, TimeUnit.MINUTES);

        try {
            String image = CaptchaGenerator.drawImage(code);
            Map<String, String> result = new HashMap<>();
            result.put("captchaKey", key);
            result.put("captchaImage", image);
            return result;
        } catch (Exception e) {
            redisUtils.delete(CAPTCHA_PREFIX + key);
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

        // 统计关注/粉丝/动态数
        vo.setFollowerCount(followMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFolloweeId, userId)).intValue());
        vo.setFollowingCount(followMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId)).intValue());
        vo.setPostCount(userPostStatsQuery.countPosts(userId));

        return vo;
    }

    @Override
    public void updateProfile(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (StringUtils.hasText(vo.getNickname())) user.setNickname(vo.getNickname());
        if (vo.getAvatar() != null) user.setAvatar(vo.getAvatar());
        if (vo.getSignature() != null) user.setSignature(vo.getSignature());
        if (vo.getSchool() != null) user.setSchool(vo.getSchool());
        if (vo.getGender() != null) user.setGender(vo.getGender());
        if (vo.getCity() != null) user.setCity(vo.getCity());
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
        String stored = redisUtils.get(SMS_CODE_PREFIX + "login:" + phone);
        if (stored == null) stored = redisUtils.get(SMS_CODE_PREFIX + "register:" + phone);
        if (stored == null) throw new BusinessException(ResultCode.SMS_CODE_EXPIRED);
        if (!stored.equals(code)) throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        redisUtils.delete(SMS_CODE_PREFIX + "login:" + phone);
        redisUtils.delete(SMS_CODE_PREFIX + "register:" + phone);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setSchool(user.getSchool());
        vo.setSignature(user.getSignature());
        vo.setCity(user.getCity());
        vo.setRealNameVerified(user.getRealNameVerified());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
