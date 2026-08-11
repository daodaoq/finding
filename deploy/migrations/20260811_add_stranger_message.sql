-- 陌生人打招呼消息表:可重复执行(适用于已部署数据库)
CREATE TABLE IF NOT EXISTS `stranger_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_user_id` BIGINT NOT NULL COMMENT '发送方',
    `to_user_id` BIGINT NOT NULL COMMENT '接收方',
    `content` VARCHAR(1000) NOT NULL COMMENT '消息内容',
    `message_type` VARCHAR(10) NOT NULL DEFAULT 'text',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待确认 1=已确认(已转为正式会话)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_to` (`from_user_id`, `to_user_id`),
    KEY `idx_to_user` (`to_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
