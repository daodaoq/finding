package com.finding.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配对记录 —— 双方互相心动后生成。userAId < userBId(规范化排序,唯一键 uk_pair)。
 */
@Data
@TableName("user_match")
public class UserMatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userAId;
    private Long userBId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime matchedAt;
}
