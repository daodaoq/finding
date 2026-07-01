package com.finding.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostVO {

    private Long id;
    private Long userId;
    private String content;
    private List<String> images;
    private String location;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer isHot;
    private Integer isTop;

    // Author info
    private UserVO author;

    // Current user interaction state
    private Boolean isLiked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
