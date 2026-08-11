package com.finding.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相识卡片展示项配置 —— 每用户一份,控制自己的卡片在推荐流中展示哪些字段。
 * 各开关 0=隐藏 1=显示(默认全开)。
 */
@Data
@TableName("user_card_config")
public class UserCardConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer showPhoto;       // 照片
    private Integer showNickname;    // 昵称
    private Integer showGender;      // 性别
    private Integer showSchool;      // 学校
    private Integer showCity;        // 城市
    private Integer showDistance;    // 距离
    private Integer showSignature;   // 自我介绍
    private Integer showMatchReasons;// 匹配理由
    private Integer showLastOnline;  // 最近在线

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
