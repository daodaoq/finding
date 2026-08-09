package com.finding.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户全局设置 —— 每用户一份(聊天通用/加好友方式/个人权限)。
 */
@Data
@TableName("user_settings")
public class UserSettings {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String chatBg;              // 全局默认聊天背景(preset key 或图片URL)
    private Integer chatMuted;          // 全局默认免打扰 0=否 1=是
    private Integer friendAddMode;      // 加好友方式 0=所有人可申请 1=需验证(默认) 2=不允许申请
    private Integer profileVisible;     // 主页可见性 1=所有人 2=仅已互换(预留)
    private Integer searchable;         // 是否可被搜索 1=是 0=否

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
