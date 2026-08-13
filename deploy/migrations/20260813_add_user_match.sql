-- 双向 match:心动(user_like) + 配对(user_match)
-- 心动是单向喜欢,双方互相心动即配对(match)。

CREATE TABLE IF NOT EXISTS `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `liker_id` BIGINT NOT NULL COMMENT '喜欢者',
  `liked_id` BIGINT NOT NULL COMMENT '被喜欢者',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_liker_liked` (`liker_id`, `liked_id`),
  KEY `idx_liked` (`liked_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_match` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_a_id` BIGINT NOT NULL COMMENT '较小 userId(规范化排序)',
  `user_b_id` BIGINT NOT NULL COMMENT '较大 userId',
  `matched_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`user_a_id`, `user_b_id`),
  KEY `idx_user_a` (`user_a_id`, `matched_at`),
  KEY `idx_user_b` (`user_b_id`, `matched_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
