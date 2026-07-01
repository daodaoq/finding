package com.finding.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    private String images;          // JSON array of URLs
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
    private Integer status;         // 0=deleted, 1=active, 2=hidden

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
