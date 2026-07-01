package com.finding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PostCreateDTO {

    @NotBlank(message = "动态内容不能为空")
    @Size(max = 5000, message = "内容最多5000字")
    private String content;

    private List<String> images;
    private String location;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
