-- 情感简历真实照片(展示用,非头像):幂等迁移(MySQL 8.0)
-- 注意:MySQL 8.0 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS(MariaDB 语法),
-- 改用 information_schema 判断列是否已存在,可重复执行(deploy.sh 每次部署都会重跑全部迁移)。
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_resume' AND COLUMN_NAME = 'real_photo');

SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `user_resume` ADD COLUMN `real_photo` VARCHAR(500) DEFAULT NULL COMMENT ''真实照片 URL'' AFTER `user_id`',
    'SELECT 1');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
