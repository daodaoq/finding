package com.finding.service;

import com.finding.dto.LoginDTO;
import com.finding.dto.RegisterDTO;
import com.finding.vo.UserVO;

import java.util.Map;

public interface AuthService {

    /** Login by password or SMS code. Returns access+refresh tokens. */
    Map<String, String> login(LoginDTO dto);

    /** Register a new account with phone + SMS code. */
    void register(RegisterDTO dto);

    /** Send SMS verification code. */
    void sendCode(String phone, String type);

    /** Refresh access token using refresh token. */
    String refreshToken(String refreshToken);

    /** Logout: blacklist current token. */
    void logout(String accessToken);

    /** Get current user profile. */
    UserVO getCurrentUser(Long userId);

    /** Update profile fields (nickname, avatar, signature, school). */
    void updateProfile(Long userId, UserVO vo);

    /** Submit real-name verification. */
    void submitVerification(Long userId, String realName, String studentId, String school,
                            String idCardFront, String idCardBack, String studentCard);
}
