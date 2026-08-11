package com.finding.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相亲交友偏好 —— 每用户一份,推荐候选资格过滤依据(不用用户自身性别代替偏好)。
 */
@Data
@TableName("user_match_preference")
public class UserMatchPreference {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 0=不限 1=男 2=女 */
    private Integer preferGender;
    /** 最小年龄,0=不限 */
    private Integer minAge;
    /** 最大年龄,0=不限 */
    private Integer maxAge;
    /** 最大距离km,0=不限 */
    private Integer maxDistanceKm;
    /** 只看已认证 0=否 1=是 */
    private Integer onlyVerified;
    /** 偏好城市,空=不限 */
    private String preferCity;
    /** 偏好目标 0=不限 1=找对象 2=交朋友 */
    private Integer preferTargetType;
    /** 资料完整度最低门槛 0-10,0=不限 */
    private Integer minCompleteness;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
