-- 图片内容审核记录(机器判定 + 送审复核队列)
CREATE TABLE IF NOT EXISTS `image_moderation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT DEFAULT NULL COMMENT '上传者(未登录为空)',
    `image_url` VARCHAR(512) DEFAULT NULL COMMENT '平台代理图片 URL',
    `scene` VARCHAR(32) DEFAULT NULL COMMENT '上传场景:avatar/profile_background/post/chat/album',
    `risk_level` VARCHAR(16) DEFAULT NULL COMMENT '阿里云返回风险等级',
    `ocr_text` VARCHAR(2000) DEFAULT NULL COMMENT 'OCR 识别文字',
    `verdict` TINYINT NOT NULL DEFAULT 0 COMMENT '机器判定:0=通过 1=拦截 2=送审',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '复核状态:0=待复核 1=已放行 2=已删除',
    `review_by` BIGINT DEFAULT NULL COMMENT '复核人',
    `review_note` VARCHAR(500) DEFAULT NULL,
    `review_time` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_verdict_status` (`verdict`, `status`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
