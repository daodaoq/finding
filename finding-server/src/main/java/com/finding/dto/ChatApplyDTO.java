package com.finding.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatApplyDTO {

    @NotNull(message = "目标用户ID不能为空")
    private Long toUserId;

    private String remark;
}
