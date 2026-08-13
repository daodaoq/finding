package com.finding.post.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 草稿保存 —— content 允许为空(未完成也能存)。
 */
@Data
public class PostDraftSaveDTO {

    @Size(max = 5000, message = "内容最多5000字")
    private String content;

    private List<String> images;
    private String location;
    private String city;

    /** 分类 code(PostCategory),可选 */
    private String category;

    /** 标签(最多 5 个,每个 ≤15 字),可选 */
    private List<String> tags;

    /** 可见性:0=公开 1=仅好友 2=仅自己 */
    private Integer visibility;
}
