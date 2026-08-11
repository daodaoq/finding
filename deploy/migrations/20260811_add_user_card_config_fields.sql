-- 相识卡片配置新增字段:年龄/实名认证标识/交友目标(幂等,可重复执行)
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_card_config' AND COLUMN_NAME = 'show_age');
SET @ddl = IF(@col = 0,
    'ALTER TABLE `user_card_config` ADD COLUMN `show_age` TINYINT NOT NULL DEFAULT 1 COMMENT ''年龄'' AFTER `show_nickname`',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_card_config' AND COLUMN_NAME = 'show_verified');
SET @ddl = IF(@col = 0,
    'ALTER TABLE `user_card_config` ADD COLUMN `show_verified` TINYINT NOT NULL DEFAULT 1 COMMENT ''实名认证标识'' AFTER `show_distance`',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_card_config' AND COLUMN_NAME = 'show_target_type');
SET @ddl = IF(@col = 0,
    'ALTER TABLE `user_card_config` ADD COLUMN `show_target_type` TINYINT NOT NULL DEFAULT 1 COMMENT ''交友目标'' AFTER `show_verified`',
    'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
