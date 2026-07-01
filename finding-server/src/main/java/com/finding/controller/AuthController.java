package com.finding.controller;

import com.finding.common.Result;
import com.finding.dto.LoginDTO;
import com.finding.dto.RegisterDTO;
import com.finding.dto.SendCodeDTO;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.AuthService;
import com.finding.vo.UserVO;
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
}
