package com.finding.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class BridgeQueryDTO extends PageQueryDTO {

    private String city;
    private String school;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
