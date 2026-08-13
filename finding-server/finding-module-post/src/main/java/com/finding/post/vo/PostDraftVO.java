package com.finding.post.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 草稿返回体(镜像保存 DTO + 更新时间)。
 */
@Data
public class PostDraftVO {

    private String content;
    private List<String> images;
    private String location;
    private String city;
    private String category;
    private List<String> tags;
    private Integer visibility;
    private LocalDateTime updatedAt;
}
