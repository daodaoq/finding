package com.finding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MateCreateDTO {

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多100字")
    private String title;

    @Size(max = 2000, message = "描述最多2000字")
    private String description;

    @NotNull(message = "活动时间不能为空")
    private LocalDateTime activityTime;

    @NotBlank(message = "地点不能为空")
    private String location;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer maxParticipants = 10;
    private Integer isAnonymous = 0;
}
