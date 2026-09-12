package com.finding.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户备注(私密别名)—— 单向:user_id 给自己通讯录里的 target_user_id 起的别名。
 * 仅设置者本人视角可见:展示层以备注替换对方昵称,不影响对方的真实昵称与任何他人视角。
 */
@Data
@TableName("user_remark")
public class UserRemark {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 备注设置者 */
    private Long userId;

    /** 被备注用户 */
    private Long targetUserId;

    /** 备注名(应用层限 20 字) */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
