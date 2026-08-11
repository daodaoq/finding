package com.finding.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 相亲"不感兴趣"排除记录 */
@Data
@TableName("recommend_exclude")
public class RecommendExclude {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long targetUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
