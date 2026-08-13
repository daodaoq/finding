-- 发帖草稿:每用户一份(user_id 唯一)
CREATE TABLE IF NOT EXISTS `post_draft` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `content` TEXT,
  `images` TEXT COMMENT 'JSON array of URLs',
  `location` VARCHAR(255) DEFAULT NULL,
  `city` VARCHAR(64) DEFAULT NULL,
  `category` VARCHAR(32) DEFAULT NULL,
  `tags` VARCHAR(512) DEFAULT NULL COMMENT '逗号分隔标签',
  `visibility` TINYINT NOT NULL DEFAULT 0 COMMENT '0=公开 1=仅好友 2=仅自己',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
