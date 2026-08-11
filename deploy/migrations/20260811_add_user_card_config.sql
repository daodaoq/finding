-- 相识卡片展示项配置表:可重复执行(适用于已部署数据库)
CREATE TABLE IF NOT EXISTS `user_card_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `show_photo` TINYINT NOT NULL DEFAULT 1 COMMENT '照片 0=隐藏 1=显示',
    `show_nickname` TINYINT NOT NULL DEFAULT 1 COMMENT '昵称',
    `show_gender` TINYINT NOT NULL DEFAULT 1 COMMENT '性别',
    `show_school` TINYINT NOT NULL DEFAULT 1 COMMENT '学校',
    `show_city` TINYINT NOT NULL DEFAULT 1 COMMENT '城市',
    `show_distance` TINYINT NOT NULL DEFAULT 1 COMMENT '距离',
    `show_signature` TINYINT NOT NULL DEFAULT 1 COMMENT '自我介绍',
    `show_match_reasons` TINYINT NOT NULL DEFAULT 1 COMMENT '匹配理由',
    `show_last_online` TINYINT NOT NULL DEFAULT 1 COMMENT '最近在线',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
