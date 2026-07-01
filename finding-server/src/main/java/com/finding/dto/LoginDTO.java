package com.finding.dto;

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
}
