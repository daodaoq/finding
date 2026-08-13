package com.finding.post.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发帖草稿 —— 每用户一份(user_id 唯一),保存未发布的动态内容。
 */
@Data
@TableName("post_draft")
public class PostDraft {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String content;
    private String images;          // JSON array of URLs(与 post 一致)
    private String location;
    private String city;
    private String category;        // 分类(PostCategory.code),可为空
    private String tags;            // 逗号分隔标签
    private Integer visibility;     // 0=公开 1=仅好友 2=仅自己

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
