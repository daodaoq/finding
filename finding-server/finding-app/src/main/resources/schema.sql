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
    `is_read` TINYINT DEFAULT 0,
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
    `status` TINYINT DEFAULT 0 COMMENT '0=pending, 1=approved, 2=rejected',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '申请备注',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handle_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_to` (`from_user_id`, `to_user_id`),
    KEY `idx_to_user_status` (`to_user_id`, `status`),
    KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
