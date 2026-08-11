package com.finding.post.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 内容审核申诉(当前用于动态被拒后的申诉) */
@Data
@TableName("appeal")
public class Appeal {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** post */
    private String targetType;
    private Long targetId;
    private String reason;
    /** 0待处理 1通过 2驳回 */
    private Integer status;
    private String originalResult;
    private Long handleBy;
    private String handleNote;
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
