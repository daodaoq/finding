package com.finding.mate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminMateReviewDTO {
    @NotNull private Boolean pass;
    @Size(max = 500) private String reason;
}
