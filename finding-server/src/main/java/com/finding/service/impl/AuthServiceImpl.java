package com.finding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import com.finding.common.constant.UserStatusEnum;
import com.finding.dto.LoginDTO;
import com.finding.dto.RegisterDTO;
import com.finding.entity.User;
import com.finding.entity.UserVerification;
import com.finding.mapper.UserMapper;
import com.finding.mapper.UserVerificationMapper;
import com.finding.security.JwtTokenProvider;
import com.finding.service.AuthService;
import com.finding.utils.RedisUtils;
import com.finding.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserVerificationMapper verificationMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtils redisUtils;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_LIMIT_PREFIX = "sms:limit:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_PREFIX = "token:refresh:";

    @Override
    public Map<String, String> login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != UserStatusEnum.ACTIVE.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // Authenticate via Spring Security's AuthenticationManager
        if ("password".equals(dto.getLoginType())) {
            if (!StringUtils.hasText(dto.getPassword())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "密码不能为空");
            }
            try {
                Authentication auth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(dto.getPhone(), dto.getPassword()));
                // Use the authenticated principal
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

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // Generate tokens
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId(), null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())));
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
        verifySmsCode(dto.getPhone(), dto.getSmsCode());

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

        log.info("New user registered: id={}, phone={}", user.getId(), dto.getPhone());
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

        log.info("SMS code for {} (type={}): {}", phone, type, code);
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
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())));
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
        return toVO(user);
    }

    @Override
    public void updateProfile(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
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

    // ── Private helpers ──

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
