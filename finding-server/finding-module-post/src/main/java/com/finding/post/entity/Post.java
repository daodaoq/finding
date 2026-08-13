package com.finding.post.entity;

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
    private String category;        // 分类(PostCategory.code)
    private String tags;            // 逗号分隔标签
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer isHot;
    private Integer isTop;
    private Integer status;         // 0=deleted, 1=active, 2=hidden
    private Integer reviewStatus;   // 0=已发布 1=待审 2=拒绝
    private String reviewReason;    // 审核拒绝原因
    private Long reviewBy;          // 审核人
    private LocalDateTime reviewTime; // 审核时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
