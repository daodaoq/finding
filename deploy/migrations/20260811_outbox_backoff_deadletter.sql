-- Outbox 可靠性增强:增加 next_retry_at(指数退避),status 新增 2=死信
-- 幂等:MySQL 8 不支持 ADD COLUMN IF NOT EXISTS,改用 information_schema
-- 判断列是否已存在,存在则跳过,重复执行安全(部署脚本每次都会重跑全部迁移)。

DROP PROCEDURE IF EXISTS `migrate_outbox_add_next_retry_at`;

DELIMITER $$
CREATE PROCEDURE `migrate_outbox_add_next_retry_at`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_outbox' AND COLUMN_NAME = 'next_retry_at'
    ) THEN
        ALTER TABLE `chat_outbox`
            ADD COLUMN `next_retry_at` DATETIME DEFAULT NULL
            COMMENT '下一次重试时间(指数退避)' AFTER `last_error`;
    END IF;
END$$
DELIMITER ;

CALL `migrate_outbox_add_next_retry_at`();
DROP PROCEDURE IF EXISTS `migrate_outbox_add_next_retry_at`;

-- MODIFY COLUMN 是幂等操作:重复执行仅把 status 重置为相同定义,无副作用
ALTER TABLE `chat_outbox`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待发布 1=已发布 2=死信';
