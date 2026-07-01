package com.finding.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** 评论视图对象 —— 支持二级嵌套回复 */
@Data
public class CommentVO {

    private Long id;
    private Long postId;
    private Long userId;
    private String nickname;
    private String avatar;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Boolean isLiked;
    private LocalDateTime createdAt;

    /** 子回复列表（仅一级评论有） */
    private List<CommentVO> replies;
    /** 回复总数（超过3条时显示"查看全部"） */
    private Integer replyCount;
}
