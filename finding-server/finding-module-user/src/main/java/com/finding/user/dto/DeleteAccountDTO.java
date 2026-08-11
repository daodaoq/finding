package com.finding.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 注销账号请求体:需二次输入密码确认,防止误删/盗用 */
@Data
public class DeleteAccountDTO {

    @NotBlank(message = "请输入密码以确认注销")
    private String password;
}
