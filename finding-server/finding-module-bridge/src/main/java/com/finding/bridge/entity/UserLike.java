package com.finding.bridge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心动(单向喜欢)—— likerId 喜欢 likedId;双方互相心动即配对(见 {@link UserMatch})。
 */
@Data
@TableName("user_like")
public class UserLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long likerId;
    private Long likedId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
