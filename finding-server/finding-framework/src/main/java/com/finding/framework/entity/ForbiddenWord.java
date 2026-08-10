package com.finding.framework.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 违禁词 —— 管理员动态维护,内容发布全链路拦截。
 */
@Data
@TableName("forbidden_word")
public class ForbiddenWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 违禁词(唯一) */
    private String word;

    /** 1=启用 0=禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
