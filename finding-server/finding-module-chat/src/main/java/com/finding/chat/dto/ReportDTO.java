package com.finding.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportDTO {

    @NotNull(message = "被投诉用户不能为空")
    private Long targetUserId;

    /** 关联会话(可选) */
    private Long roomId;

    @NotBlank(message = "投诉原因不能为空")
    private String reason;
}
