-- ============================================================
-- Finding 大学生社交平台 - 数据库初始化 (schema only)
-- 说明: 原 init.sql 为编码损坏的数据库导出(UTF-16/GBK 混合乱码),
--       无法被 MySQL 解析。现替换为项目 resources/schema.sql 的干净结构脚本。
-- 建库 + 建 26 张表;如需测试数据请另行导入正确编码的数据文件。
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- Finding 大学生社交平台 - Database Schema
-- MySQL 8.0+, InnoDB, utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS finding DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE finding;

-- ============================================================
-- 1. user - Core user identity
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `nickname` VARCHAR(50) NOT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `gender` TINYINT DEFAULT 0 COMMENT '0=unknown, 1=male, 2=female',
    `birthday` DATE DEFAULT NULL,
    `school` VARCHAR(100) DEFAULT NULL,
    `student_id` VARCHAR(50) DEFAULT NULL,
    `signature` VARCHAR(200) DEFAULT NULL,
    `city` VARCHAR(50) DEFAULT NULL,
    `latitude` DECIMAL(10,7) DEFAULT NULL,
    `longitude` DECIMAL(10,7) DEFAULT NULL,
    `role` VARCHAR(20) DEFAULT 'user' COMMENT 'user / admin',
    `status` TINYINT DEFAULT 1 COMMENT '0=banned, 1=active, 2=frozen',
    `banned_until` DATETIME DEFAULT NULL COMMENT '封禁到期时间(NULL=永久封禁)',
    `banned_reason` VARCHAR(500) DEFAULT NULL COMMENT '封禁原因',
    `real_name_verified` TINYINT DEFAULT 0 COMMENT '0=no, 1=pending, 2=approved, 3=rejected',
    `last_login_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_school` (`school`),
    KEY `idx_location` (`latitude`, `longitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. user_verification - Real-name verification audit trail
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_verification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `real_name` VARCHAR(50) NOT NULL,
    `student_id` VARCHAR(50) NOT NULL,
    `school` VARCHAR(100) NOT NULL,
    `id_card_front` VARCHAR(500) DEFAULT NULL,
    `id_card_back` VARCHAR(500) DEFAULT NULL,
    `student_card` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT DEFAULT 0 COMMENT '0=pending, 1=approved, 2=rejected',
    `reviewer_id` BIGINT DEFAULT NULL,
    `review_comment` VARCHAR(500) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. post - Square feed posts
-- ============================================================
CREATE TABLE IF NOT EXISTS `post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `content` TEXT,
    `images` VARCHAR(2000) DEFAULT NULL COMMENT 'comma-separated image URLs',
    `location` VARCHAR(100) DEFAULT NULL,
    `city` VARCHAR(50) DEFAULT NULL,
    `latitude` DECIMAL(10,7) DEFAULT NULL,
    `longitude` DECIMAL(10,7) DEFAULT NULL,
    `view_count` INT DEFAULT 0,
    `like_count` INT DEFAULT 0,
    `comment_count` INT DEFAULT 0,
    `share_count` INT DEFAULT 0,
    `is_hot` TINYINT DEFAULT 0,
    `is_top` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '0=deleted, 1=active, 2=hidden',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_hot` (`is_hot`, `like_count`, `created_at`),
    KEY `idx_city` (`city`, `created_at`),
    KEY `idx_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. post_like
-- ============================================================
CREATE TABLE IF NOT EXISTS `post_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4a. post_comment_like
-- ============================================================
CREATE TABLE IF NOT EXISTS `post_comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `comment_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. post_comment
-- ============================================================
CREATE TABLE IF NOT EXISTS `post_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT NULL COMMENT 'NULL=top-level, references self for replies',
    `content` VARCHAR(1000) NOT NULL,
    `like_count` INT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_comment_post` (`post_id`, `created_at`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. user_follow
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `follower_id` BIGINT NOT NULL COMMENT 'Who follows',
    `followee_id` BIGINT NOT NULL COMMENT 'Who is followed',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`follower_id`, `followee_id`),
    KEY `idx_followee` (`followee_id`),
    KEY `idx_follower` (`follower_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. mate_invitation - Mate/搭子 invitations
-- ============================================================
CREATE TABLE IF NOT EXISTS `mate_invitation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'Creator',
    `category` VARCHAR(20) NOT NULL COMMENT 'travel/carpool/fitness/study/exam/sports/gaming/entertainment/other',
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `activity_time` DATETIME DEFAULT NULL,
    `location` VARCHAR(200) DEFAULT NULL,
    `latitude` DECIMAL(10,7) DEFAULT NULL,
    `longitude` DECIMAL(10,7) DEFAULT NULL,
    `max_participants` INT DEFAULT 10,
    `current_participants` INT DEFAULT 1,
    `is_anonymous` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1 COMMENT '0=cancelled, 1=active, 2=closed',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`, `status`),
    KEY `idx_location` (`latitude`, `longitude`),
    KEY `idx_activity_time` (`activity_time`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status_time` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. mate_participant
-- ============================================================
CREATE TABLE IF NOT EXISTS `mate_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `invitation_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` TINYINT DEFAULT 0 COMMENT '0=pending, 1=accepted, 2=rejected',
    `message` VARCHAR(500) DEFAULT NULL COMMENT 'Applicant message',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mate_user` (`invitation_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. message - Notifications
-- ============================================================
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_user_id` BIGINT DEFAULT NULL COMMENT 'NULL for system messages',
    `to_user_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL COMMENT 'like/comment/follow/mate_request/mate_accepted/mate_rejected/system',
    `content` VARCHAR(500) DEFAULT NULL,
    `related_id` BIGINT DEFAULT NULL COMMENT 'post_id or invitation_id',
    `is_read` TINYINT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_msg_to_user` (`to_user_id`, `is_read`, `created_at`),
    KEY `idx_msg_type` (`to_user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. conversation - One-on-one chat sessions
-- ============================================================
CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user1_id` BIGINT NOT NULL COMMENT 'Smaller user ID',
    `user2_id` BIGINT NOT NULL COMMENT 'Larger user ID',
    `last_message` VARCHAR(500) DEFAULT NULL,
    `last_message_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation` (`user1_id`, `user2_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. private_chat - Chat messages
-- ============================================================
CREATE TABLE IF NOT EXISTS `private_chat` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'deprecated, use room_id',
    `room_id` BIGINT DEFAULT NULL COMMENT 'FK to room.id',
    `from_user_id` BIGINT NOT NULL,
    `to_user_id` BIGINT NOT NULL,
    `content` TEXT,
    `message_type` VARCHAR(10) DEFAULT 'text' COMMENT 'text / image',
    `is_recalled` TINYINT NOT NULL DEFAULT 0 COMMENT '0=否 1=已撤回',
    `is_read` TINYINT DEFAULT 0,
    `uid1_hidden` TINYINT NOT NULL DEFAULT 0 COMMENT 'uid1(较小者)是否已单侧清空',
    `uid2_hidden` TINYINT NOT NULL DEFAULT 0 COMMENT 'uid2(较大者)是否已单侧清空',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chat_conv` (`conversation_id`, `created_at`),
    KEY `idx_chat_room` (`room_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11a. room (MallChat 聊天容器)
-- ============================================================
CREATE TABLE IF NOT EXISTS `room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `type` INT DEFAULT 1 COMMENT '1=单聊, 2=群聊',
    `hot_flag` INT DEFAULT 0,
    `active_time` DATETIME DEFAULT NULL,
    `last_msg_id` BIGINT DEFAULT NULL,
    `ext_json` TEXT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11b. room_friend (单聊房间关联)
-- ============================================================
CREATE TABLE IF NOT EXISTS `room_friend` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL,
    `uid1` BIGINT NOT NULL,
    `uid2` BIGINT NOT NULL,
    `room_key` VARCHAR(64) NOT NULL,
    `status` INT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_key` (`room_key`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11c. contact (用户-会话关联)
-- ============================================================
CREATE TABLE IF NOT EXISTS `contact` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `uid` BIGINT NOT NULL,
    `room_id` BIGINT NOT NULL,
    `read_time` DATETIME DEFAULT NULL,
    `active_time` DATETIME DEFAULT NULL,
    `last_msg_id` BIGINT DEFAULT NULL,
    `pinned` TINYINT DEFAULT 0 COMMENT '0=否 1=置顶',
    `muted` TINYINT DEFAULT 0 COMMENT '0=否 1=消息免打扰',
    `background` VARCHAR(500) DEFAULT NULL COMMENT '聊天背景(preset key 或图片URL)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_uid_room` (`uid`, `room_id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11d. room_group (群聊房间关联)
-- ============================================================
CREATE TABLE IF NOT EXISTS `room_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `owner_uid` BIGINT NOT NULL,
    `announcement` VARCHAR(500) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11e. group_member (群成员)
-- ============================================================
CREATE TABLE IF NOT EXISTS `group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `uid` BIGINT NOT NULL,
    `role` TINYINT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11f. message_mark (消息标记)
-- ============================================================
CREATE TABLE IF NOT EXISTS `message_mark` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `msg_id` BIGINT NOT NULL,
    `uid` BIGINT NOT NULL,
    `mark_type` TINYINT NOT NULL,
    `act_type` TINYINT NOT NULL,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_msg_id` (`msg_id`),
    KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. group_chat
-- ============================================================
CREATE TABLE IF NOT EXISTS `group_chat` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `owner_id` BIGINT NOT NULL,
    `member_count` INT DEFAULT 1,
    `announcement` VARCHAR(500) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. group_chat_member
-- ============================================================
CREATE TABLE IF NOT EXISTS `group_chat_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` TINYINT DEFAULT 0 COMMENT '0=member, 1=admin, 2=owner',
    `last_read_msg_id` BIGINT NOT NULL DEFAULT 0 COMMENT '用户在该群最后已读的消息ID',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13a. group_message
-- ============================================================
CREATE TABLE IF NOT EXISTS `group_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `from_user_id` BIGINT NOT NULL,
    `content` TEXT,
    `message_type` VARCHAR(10) DEFAULT 'text',
    `is_recalled` TINYINT NOT NULL DEFAULT 0 COMMENT '0=否 1=已撤回',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group_msg` (`group_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. banner
-- ============================================================
CREATE TABLE IF NOT EXISTS `banner` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(100) DEFAULT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `link_url` VARCHAR(500) DEFAULT NULL,
    `sort_order` INT DEFAULT 0,
    `is_active` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_active_sort` (`is_active`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. system_announcement
-- ============================================================
CREATE TABLE IF NOT EXISTS `system_announcement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '1=普通公告(弹窗) 2=永久展示(顶部横条)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=展示中 0=已下架',
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 16. vip_record
-- ============================================================
CREATE TABLE IF NOT EXISTS `vip_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `level` INT DEFAULT 1,
    `start_date` DATE DEFAULT NULL,
    `end_date` DATE DEFAULT NULL,
    `amount` DECIMAL(10,2) DEFAULT NULL,
    `payment_method` VARCHAR(20) DEFAULT NULL,
    `status` TINYINT DEFAULT 1 COMMENT '0=expired, 1=active',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 17. recharge_record
-- ============================================================
CREATE TABLE IF NOT EXISTS `recharge_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(10,2) NOT NULL,
    `payment_method` VARCHAR(20) DEFAULT NULL,
    `transaction_id` VARCHAR(100) DEFAULT NULL,
    `status` TINYINT DEFAULT 0 COMMENT '0=pending, 1=success, 2=failed',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 18. chat_apply - Chat application requests (鹊桥心动申请)
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_apply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_user_id` BIGINT NOT NULL COMMENT '申请人',
    `to_user_id` BIGINT NOT NULL COMMENT '接收人',
    `status` TINYINT DEFAULT 0 COMMENT '0=pending, 1=approved, 2=rejected, 3=cancelled, 4=expired',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '申请备注',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `handle_by` BIGINT DEFAULT NULL COMMENT '处理人用户ID(撤回/审批/系统过期)',
    -- 仅待处理申请非空(等于 from_to),唯一约束保证同一方向同时只有一条待处理申请;
    -- 审批/撤回/过期后变 NULL,允许同方向在冷却期后重新申请
    `pending_key` VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN `status` = 0 THEN CONCAT(`from_user_id`, '_', `to_user_id`) ELSE NULL END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pending_key` (`pending_key`),
    KEY `idx_to_user_status` (`to_user_id`, `status`),
    KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 19. user_resume - 情感简历 (每个用户一份)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_resume` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    -- 板块1 基础信息
    `gender` TINYINT DEFAULT NULL COMMENT '1=男 2=女',
    `age` INT DEFAULT NULL,
    `birthday` DATE DEFAULT NULL,
    `constellation` VARCHAR(20) DEFAULT NULL,
    `height_cm` INT DEFAULT NULL,
    `weight_kg` INT DEFAULT NULL,
    `campus` VARCHAR(100) DEFAULT NULL,
    `major_grade` VARCHAR(100) DEFAULT NULL,
    `hometown` VARCHAR(100) DEFAULT NULL,
    `career` VARCHAR(100) DEFAULT NULL COMMENT '职业',
    `daily_routine` VARCHAR(200) DEFAULT NULL COMMENT '日常作息',
    `relationship_status` VARCHAR(200) DEFAULT NULL COMMENT '恋爱状态(单身时长/恋爱期待)',
    `core_bottom_line` VARCHAR(500) DEFAULT NULL COMMENT '择偶核心底线',
    -- 板块2 自我画像
    `mbti` VARCHAR(10) DEFAULT NULL,
    `personality_traits` VARCHAR(500) DEFAULT NULL,
    `in_love_look` VARCHAR(500) DEFAULT NULL,
    `flaws` VARCHAR(500) DEFAULT NULL,
    `worldview` VARCHAR(500) DEFAULT NULL COMMENT '个人三观',
    `personal_tags` VARCHAR(500) DEFAULT NULL COMMENT '个人标签',
    -- 板块3 恋爱复盘
    `relationship_count` VARCHAR(50) DEFAULT NULL,
    `breakup_reason` VARCHAR(500) DEFAULT NULL,
    `love_shortcoming` VARCHAR(500) DEFAULT NULL,
    `love_insight` VARCHAR(500) DEFAULT NULL,
    `love_growth` VARCHAR(500) DEFAULT NULL COMMENT '自己在感情里的成长/改掉的毛病',
    -- 板块4 恋爱相处模式
    `daily_company` VARCHAR(500) DEFAULT NULL,
    `fight_mode` VARCHAR(500) DEFAULT NULL,
    `love_expression` VARCHAR(500) DEFAULT NULL,
    `opposite_boundary` VARCHAR(500) DEFAULT NULL,
    -- 板块5 个人生活与规划
    `daily_status` VARCHAR(500) DEFAULT NULL,
    `life_habits` VARCHAR(500) DEFAULT NULL,
    `short_term_plan` VARCHAR(500) DEFAULT NULL,
    `long_term_plan` VARCHAR(500) DEFAULT NULL,
    `hobbies` VARCHAR(500) DEFAULT NULL COMMENT '爱好与日常',
    `marriage_plan` VARCHAR(500) DEFAULT NULL COMMENT '长期婚恋规划',
    -- 板块6 理想的另一半
    `hard_conditions` VARCHAR(500) DEFAULT NULL,
    `soft_expectations` VARCHAR(500) DEFAULT NULL,
    -- 板块7 加分项(我能为恋爱带来什么)
    `bonus_points` VARCHAR(500) DEFAULT NULL COMMENT '情绪价值/实际付出/未来规划',
    -- 板块8 走心宣言
    `love_expectation` VARCHAR(500) DEFAULT NULL,
    `love_attitude` VARCHAR(500) DEFAULT NULL,
    -- 板块9 生活相册 (图片URL数组)
    `photo_album` JSON DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 20. user_info_share - 信息互换申请
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_info_share` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_user_id` BIGINT NOT NULL COMMENT '发起方',
    `to_user_id` BIGINT NOT NULL COMMENT '接收方',
    `status` TINYINT DEFAULT 0 COMMENT '0=pending 1=approved 2=rejected',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handled_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_to` (`from_user_id`, `to_user_id`),
    KEY `idx_to_user` (`to_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 21. report - 用户投诉
-- ============================================================
CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_user_id` BIGINT NOT NULL COMMENT '投诉人',
    `target_user_id` BIGINT NOT NULL COMMENT '被投诉人',
    `room_id` BIGINT DEFAULT NULL COMMENT '关联会话(可选)',
    `target_type` VARCHAR(20) DEFAULT NULL COMMENT 'message/post/comment/user/resume',
    `target_id` BIGINT DEFAULT NULL COMMENT '被投诉目标ID',
    `content_snapshot` TEXT COMMENT '被投诉内容快照',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '投诉原因',
    `evidence` TEXT COMMENT '证据附件(逗号分隔URL)',
    `status` TINYINT DEFAULT 0 COMMENT '0=待处理 1=已处理 2=驳回',
    `handle_by` BIGINT DEFAULT NULL COMMENT '处理人',
    `handle_note` VARCHAR(500) DEFAULT NULL COMMENT '处理结果/意见',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_user_id`),
    KEY `idx_from` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 22. view_history - 浏览记录(每用户每目标一条,重复浏览刷新时间)
-- ============================================================
CREATE TABLE IF NOT EXISTS `view_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `target_type` VARCHAR(20) NOT NULL COMMENT 'post/user',
    `target_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 23. user_settings - 用户全局设置(每用户一份)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_settings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `chat_bg` VARCHAR(500) DEFAULT NULL COMMENT '全局默认聊天背景(preset key 或图片URL)',
    `chat_muted` TINYINT DEFAULT 0 COMMENT '全局默认免打扰 0=否 1=是',
    `friend_add_mode` TINYINT DEFAULT 1 COMMENT '加好友方式 0=所有人可申请 1=需验证(默认) 2=不允许申请',
    `profile_visible` TINYINT DEFAULT 1 COMMENT '主页可见性 1=所有人 2=仅已互换(预留)',
    `searchable` TINYINT DEFAULT 1 COMMENT '是否可被搜索 1=是 0=否',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 24. forbidden_word - 违禁词(管理员动态维护,内容发布全链路拦截)
-- ============================================================
CREATE TABLE IF NOT EXISTS `forbidden_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `word` VARCHAR(100) NOT NULL COMMENT '违禁词',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 25. user_block - 拉黑/屏蔽(单向,防骚扰)
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_block` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '拉黑发起方',
    `blocked_user_id` BIGINT NOT NULL COMMENT '被拉黑用户',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_blocked` (`user_id`, `blocked_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 26. feedback - 用户反馈/客服工单
-- ============================================================
CREATE TABLE IF NOT EXISTS `feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) DEFAULT 'other' COMMENT 'bug/feature/suggestion/other',
    `content` VARCHAR(2000) NOT NULL,
    `contact` VARCHAR(100) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待处理 1=已处理',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handled_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
