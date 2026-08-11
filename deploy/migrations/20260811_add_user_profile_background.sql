-- 用户个人中心资料卡背景图：幂等迁移(MySQL 8.0)
-- 注意:MySQL 8.0 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS(MariaDB 语法),
-- 改用 information_schema 判断列是否已存在,可重复执行。
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'profile_background');

SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `profile_background` VARCHAR(500) DEFAULT NULL COMMENT ''个人中心资料卡背景图 URL'' AFTER `avatar`',
    'SELECT 1');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
