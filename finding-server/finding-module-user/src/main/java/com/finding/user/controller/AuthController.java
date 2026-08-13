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
import jakarta.servlet.http.HttpServletRequest;
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
    public Result<Map<String, String>> login(@Valid @RequestBody LoginDTO dto,
                                             HttpServletRequest request) {
        return Result.ok(authService.login(dto, clientIp(request)));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto,
                                 @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
                                 HttpServletRequest request) {
        authService.register(dto, clientIp(request), deviceId);
        return Result.ok();
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto,
                                 HttpServletRequest request) {
        authService.sendCode(dto.getPhone(), dto.getType(), clientIp(request));
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

    /** 取客户端真实 IP:优先 nginx 注入的 X-Real-IP(proxy_set_header 覆盖客户端伪造值),否则 XFF 末位,最后 remoteAddr */
    private String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
