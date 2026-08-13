package com.finding.post.dto;

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

    /** 分类 code(PostCategory),可选 */
    private String category;

    /** 标签(最多 5 个,每个 ≤15 字),可选 */
    private List<String> tags;

    /** 可见性:0=公开 1=仅好友 2=仅自己,默认公开 */
    private Integer visibility;

    private BigDecimal latitude;
    private BigDecimal longitude;
}
