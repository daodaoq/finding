-- 论坛内容组织:帖子分类 + 标签
ALTER TABLE `post`
  ADD COLUMN `category` VARCHAR(30) DEFAULT NULL COMMENT '分类(study/life/confession/lostfound/job/food/sports/other)' AFTER `city`,
  ADD COLUMN `tags` VARCHAR(255) DEFAULT NULL COMMENT '逗号分隔标签' AFTER `category`,
  ADD KEY `idx_category` (`category`, `created_at`);
