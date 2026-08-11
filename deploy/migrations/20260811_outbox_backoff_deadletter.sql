-- Outbox 可靠性增强:增加 next_retry_at(指数退避),status 新增 2=死信
ALTER TABLE `chat_outbox`
    ADD COLUMN `next_retry_at` DATETIME DEFAULT NULL COMMENT '下一次重试时间(指数退避)' AFTER `last_error`,
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待发布 1=已发布 2=死信';

-- 存量记录视为立即可重试(NULL),无需额外处理
