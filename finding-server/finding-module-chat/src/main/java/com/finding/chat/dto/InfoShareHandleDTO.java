package com.finding.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InfoShareHandleDTO {

    @NotNull(message = "操作状态不能为空")
    @Min(value = 1, message = "status: 1=同意, 2=拒绝")
    @Max(value = 2, message = "status: 1=同意, 2=拒绝")
    private Integer status; // 1=同意, 2=拒绝
}
