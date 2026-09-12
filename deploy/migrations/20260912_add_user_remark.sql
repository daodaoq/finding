-- 用户备注(私密别名):设置后仅设置者本人视角内以备注替换对方昵称展示。
-- 幂等:CREATE TABLE IF NOT EXISTS,可随 deploy.sh 重复执行。
CREATE TABLE IF NOT EXISTS `user_remark` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '备注设置者',
  `target_user_id` BIGINT NOT NULL COMMENT '被备注用户',
  `remark` VARCHAR(50) NOT NULL COMMENT '备注名(应用层限 20 字)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_user_id`),
  KEY `idx_user` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
