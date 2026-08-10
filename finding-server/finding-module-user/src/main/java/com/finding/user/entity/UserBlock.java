package com.finding.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拉黑/屏蔽记录 —— 单向:user_id 拉黑了 blocked_user_id。
 * 被拉黑后双方无法再私聊(发送方视角各查一次)。
 */
@Data
@TableName("user_block")
public class UserBlock {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拉黑发起方 */
    private Long userId;

    /** 被拉黑用户 */
    private Long blockedUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
