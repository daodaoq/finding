package com.finding.post.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 帖子收藏 */
@Data
@TableName("post_favorite")
public class PostFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
