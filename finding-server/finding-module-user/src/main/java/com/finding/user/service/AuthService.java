package com.finding.user.service;

import com.finding.user.dto.LoginDTO;
import com.finding.user.dto.RegisterDTO;
import com.finding.user.entity.UserVerification;
import com.finding.user.vo.UserVO;

import java.util.Map;

public interface AuthService {

    /** Login by password or SMS code. Returns access+refresh tokens. */
    Map<String, String> login(LoginDTO dto);

    /** 注册新账号:滑块拼图验证 + 按 IP/设备指纹防批量注册限流。 */
    void register(RegisterDTO dto, String ip, String deviceId);

    /** Send SMS verification code. */
    void sendCode(String phone, String type);

    /** 生成滑块拼图验证码,返回 captchaKey + bgImage + pieceImage + y(base64 PNG)。 */
    Map<String, String> generateCaptcha();

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

    /** 获取当前用户自己的认证记录(仅本人可见，无记录返回 null)。 */
    UserVerification getMyVerification(Long userId);

    /** 修改密码:校验旧密码后更新,并作废旧 refresh token。 */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /** 当前用户的账号信息(仅本人可见,含手机号)。 */
    Map<String, String> getAccount(Long userId);

    /** 注销账号:校验密码后匿名化资料、停用账号、撤销登录态,并联动取消待处理申请。 */
    void deleteAccount(Long userId, String password);
}
