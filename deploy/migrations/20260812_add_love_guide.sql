CREATE TABLE IF NOT EXISTS `love_guide` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL,
  `title` VARCHAR(60) NOT NULL, `subtitle` VARCHAR(100) NOT NULL, `content` TEXT NOT NULL, `category` VARCHAR(30) NOT NULL,
  `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=pending,1=approved,2=rejected', `review_reason` VARCHAR(500) DEFAULT NULL,
  `review_by` BIGINT DEFAULT NULL, `review_time` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_review_created` (`review_status`, `created_at`), KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
