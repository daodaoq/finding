package com.finding.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageQueryDTO {

    /** "hot", "latest", "following" */
    private String tab = "hot";
    private String city;
}
