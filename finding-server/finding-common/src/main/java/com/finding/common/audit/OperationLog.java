package com.finding.common.audit;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 敏感操作审计日志(封禁/举报处理/内容审核/搭子处置等) */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作者 */
    private Long operatorId;
    /** 动作:ban/report_handle/post_review/mate_status */
    private String action;
    /** 目标类型:user/report/post/mate */
    private String targetType;
    private Long targetId;
    /** 操作详情 */
    private String detail;
    /** 结果/备注 */
    private String result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
