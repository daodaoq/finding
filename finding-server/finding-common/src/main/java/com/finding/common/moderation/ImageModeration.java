package com.finding.common.moderation;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图片内容审核记录 —— 上传时记录机器判定(通过/拦截/送审),
 * 送审(review)进入后台复核队列,拦截(block)与通过(pass)仅留审计痕迹。
 */
@Data
@TableName("image_moderation")
public class ImageModeration {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 上传者(未登录时为空) */
    private Long userId;
    /** 平台代理图片 URL */
    private String imageUrl;
    /** 上传场景:avatar/profile_background/post/chat/album */
    private String scene;
    /** 阿里云内容安全返回风险等级 */
    private String riskLevel;
    /** OCR 识别文字(供人工复核) */
    private String ocrText;
    /** 机器判定:0=通过 1=拦截 2=送审 */
    private Integer verdict;
    /** 复核状态:0=待复核 1=已放行 2=已删除 */
    private Integer status;
    /** 复核人 */
    private Long reviewBy;
    private String reviewNote;
    private LocalDateTime reviewTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
