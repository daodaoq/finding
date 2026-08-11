package com.finding.user.controller;

import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.user.dto.DeleteAccountDTO;
import com.finding.user.dto.LoginDTO;
import com.finding.user.dto.RegisterDTO;
import com.finding.user.dto.SendCodeDTO;
import com.finding.user.entity.UserVerification;
import com.finding.user.security.JwtInterceptor;
import com.finding.user.service.AuthService;
import com.finding.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        authService.sendCode(dto.getPhone(), dto.getType());
        return Result.ok();
    }

    /** 生成图片验证码(注册用),返回 captchaKey + captchaImage(base64 PNG) */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(authService.generateCaptcha());
    }

    @PostMapping("/refresh")
    public Result<String> refresh(@RequestParam String refreshToken) {
        return Result.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return Result.ok();
    }

    /** 注销账号:校验密码后匿名化并停用,撤销登录态 */
    @PostMapping("/delete-account")
    public Result<Void> deleteAccount(@Valid @RequestBody DeleteAccountDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        authService.deleteAccount(userId, dto.getPassword());
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.getCurrentUser(JwtInterceptor.getCurrentUserId()));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserVO vo) {
        authService.updateProfile(JwtInterceptor.getCurrentUserId(), vo);
        return Result.ok();
    }

    @PostMapping("/verify")
    public Result<Void> submitVerification(@RequestParam String realName,
                                           @RequestParam String studentId,
                                           @RequestParam String school,
                                           @RequestParam(required = false) String idCardFront,
                                           @RequestParam(required = false) String idCardBack,
                                           @RequestParam(required = false) String studentCard) {
        authService.submitVerification(JwtInterceptor.getCurrentUserId(),
                realName, studentId, school, idCardFront, idCardBack, studentCard);
        return Result.ok();
    }

    /** 获取当前用户自己的认证记录(仅本人可见) */
    @GetMapping("/verification")
    public Result<UserVerification> getMyVerification() {
        return Result.ok(authService.getMyVerification(JwtInterceptor.getCurrentUserId()));
    }

    /** 修改密码 */
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(JwtInterceptor.getCurrentUserId(),
                body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    /** 当前用户账号信息(仅本人可见,含手机号) */
    @GetMapping("/account")
    public Result<Map<String, String>> account() {
        return Result.ok(authService.getAccount(JwtInterceptor.getCurrentUserId()));
    }
}
