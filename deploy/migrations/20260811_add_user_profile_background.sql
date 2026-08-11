-- 用户个人中心资料卡背景图：可重复执行，适用于已部署数据库。
ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `profile_background` VARCHAR(500) DEFAULT NULL COMMENT '个人中心资料卡背景图 URL' AFTER `avatar`;
