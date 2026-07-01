package com.finding.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQueryDTO {

    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @Min(value = 1, message = "每页最少1条")
    @Max(value = 50, message = "每页最多50条")
    private Integer size = 10;

    private String keyword;

    /**
     * For cursor-based pagination: the ID of the last item from the previous page.
     */
    private Long lastId;
}
