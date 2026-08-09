package com.finding.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** Login type: "password" or "sms" */
    @NotBlank(message = "登录类型不能为空")
    private String loginType;

    /** Password (when loginType=password) */
    private String password;

    /** SMS code (when loginType=sms) */
    private String smsCode;

    // ── 自定义 setter:登录时统一去掉文本两端空格(校验前生效) ──

    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType == null ? null : loginType.trim();
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode == null ? null : smsCode.trim();
    }
}
