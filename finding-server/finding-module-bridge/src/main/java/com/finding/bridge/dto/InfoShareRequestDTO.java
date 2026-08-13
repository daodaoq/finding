package com.finding.bridge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InfoShareRequestDTO {

    @NotNull(message = "目标用户不能为空")
    private Long toUserId;
}
