-- 情感简历开关:0=关闭(默认) 1=开启
-- 语义:未开启的用户不参与相识/心动推荐的匹配,也不能主动心动或发聊天申请。
--
-- 幂等:MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS,用 information_schema 判断列是否存在,
-- 存在则跳过,可重复执行(deploy.sh 每次部署都会重跑全部迁移)。
--
-- 存量数据:不做回填,已有记录一律保持 0(关闭)。原因是现有记录均不满足核心 10 项必填
-- (真实照片/性别/生日/身高/体重/校区/MBTI/性格/三观/择偶底线),若回填为 1 会把不完整
-- 简历放进推荐池,正是本功能要消除的问题。数据保留,由用户自行开启。

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_resume' AND COLUMN_NAME = 'enabled');

SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `user_resume` ADD COLUMN `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT ''情感简历开关 0=关闭(默认) 1=开启'' AFTER `user_id`',
    'SELECT 1');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
