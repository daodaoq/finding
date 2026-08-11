package com.finding.mate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminMateStatusDTO {
    @NotNull @Min(0) @Max(3)
    private Integer status;
}
