package com.finding.mate.dto;

import com.finding.common.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;

@Data
@EqualsAndHashCode(callSuper = true)
public class MateQueryDTO extends PageQueryDTO {

    private String category;
    private String city;
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private BigDecimal latitude;
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal longitude;
    @DecimalMin(value = "0.1") @DecimalMax(value = "200.0")
    private Double radiusKm;
    private Integer status; // 可选: null=全部, 1=进行中, 2=已结束
    private Boolean anonymousOnly;
    @Min(1) @Max(30)
    private Integer daysAhead;
    private Boolean availableOnly;

    @AssertTrue(message = "经纬度必须同时填写，半径筛选必须提供定位")
    public boolean isGeoQueryValid() {
        boolean paired = (latitude == null) == (longitude == null);
        return paired && (radiusKm == null || latitude != null);
    }
}
