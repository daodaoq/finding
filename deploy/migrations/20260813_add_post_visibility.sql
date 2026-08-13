-- 帖子可见性:0=公开 1=仅好友 2=仅自己
ALTER TABLE `post`
  ADD COLUMN `visibility` TINYINT NOT NULL DEFAULT 0 COMMENT '0=公开 1=仅好友 2=仅自己' AFTER `status`;
