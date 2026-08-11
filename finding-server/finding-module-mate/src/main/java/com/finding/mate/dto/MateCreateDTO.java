package com.finding.mate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MateCreateDTO {

    @NotBlank(message = "分类不能为空")
    @Pattern(regexp = "travel|carpool|fitness|study|exam|sports|gaming|entertainment|other", message = "不支持的活动分类")
    private String category;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多100字")
    private String title;

    @Size(max = 2000, message = "描述最多2000字")
    private String description;

    @NotNull(message = "活动时间不能为空")
    private LocalDateTime activityTime;

    @NotBlank(message = "地点不能为空")
    @Size(max = 200, message = "地点最长200字")
    private String location;

    @DecimalMin(value = "-90.0", message = "纬度范围应为 -90 到 90")
    @DecimalMax(value = "90.0", message = "纬度范围应为 -90 到 90")
    private BigDecimal latitude;
    @DecimalMin(value = "-180.0", message = "经度范围应为 -180 到 180")
    @DecimalMax(value = "180.0", message = "经度范围应为 -180 到 180")
    private BigDecimal longitude;
    @NotNull(message = "人数上限不能为空")
    @Min(value = 2, message = "人数上限至少为2")
    @Max(value = 50, message = "人数上限不能超过50")
    private Integer maxParticipants = 10;
    @NotNull(message = "匿名设置不能为空")
    @Min(value = 0, message = "匿名设置只能为0或1")
    @Max(value = 1, message = "匿名设置只能为0或1")
    private Integer isAnonymous = 0;
}
