package com.finding.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageQueryDTO {

    /** "hot", "latest", "following" */
    private String tab = "hot";

    /** 热门子排序: views(浏览量最高), likes(点赞率最高), recommended(值得推荐) */
    private String sortBy;

    private String city;
}
