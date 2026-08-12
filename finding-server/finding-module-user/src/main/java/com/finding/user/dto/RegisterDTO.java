package com.finding.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 滑块拼图最终 X(拼图块左边缘,相对 300px 原图) */
    @NotNull(message = "请完成滑块验证")
    private Integer captchaX;

    /** 拖动耗时(ms),行为校验用 */
    private Long captchaTime;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长50个字符")
    private String nickname;

    private String school;
    private Integer gender;
}
