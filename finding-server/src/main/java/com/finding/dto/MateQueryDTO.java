package com.finding.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class MateQueryDTO extends PageQueryDTO {

    private String category;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double radiusKm;
    private Integer status; // 可选: null=全部, 1=进行中, 2=已结束
}
