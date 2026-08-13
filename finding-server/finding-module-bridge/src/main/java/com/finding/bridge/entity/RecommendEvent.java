package com.finding.bridge.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 相亲行为事件(匿名统计):expose/skip/apply/approve */
@Data
@TableName("recommend_event")
public class RecommendEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** expose=曝光 skip=跳过 apply=申请 approve=通过 */
    private String eventType;
    private Long targetUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
