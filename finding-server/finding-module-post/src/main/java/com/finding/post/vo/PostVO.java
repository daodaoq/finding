package com.finding.post.vo;

import com.finding.user.vo.UserVO;
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
    /** 分类 code */
    private String category;
    /** 分类中文描述 */
    private String categoryDesc;
    /** 标签列表 */
    private List<String> tags;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer isHot;
    private Integer isTop;
    /** 审核状态:0=已发布 1=待审 2=拒绝(作者可见) */
    private Integer reviewStatus;
    /** 审核拒绝原因 */
    private String reviewReason;

    // Author info
    private UserVO author;

    // Current user interaction state
    private Boolean isLiked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
