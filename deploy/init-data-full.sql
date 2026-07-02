mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: finding
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `banner`
--

DROP TABLE IF EXISTS `banner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `link_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `is_active` tinyint DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_active_sort` (`is_active`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `banner`
--

LOCK TABLES `banner` WRITE;
/*!40000 ALTER TABLE `banner` DISABLE KEYS */;
INSERT INTO `banner` (`id`, `title`, `image_url`, `link_url`, `sort_order`, `is_active`, `created_at`) VALUES (1,'新生交友季','https://picsum.photos/800/300?random=1','/mate',1,1,'2026-07-01 20:23:32'),(2,'搭子匹配上线','https://picsum.photos/800/300?random=2','/mate',2,1,'2026-07-01 20:23:32'),(3,'实名认证指南','https://picsum.photos/800/300?random=3','/mine/verify',3,1,'2026-07-01 20:23:32');
/*!40000 ALTER TABLE `banner` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_apply`
--

DROP TABLE IF EXISTS `chat_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL COMMENT '申请人',
  `to_user_id` bigint NOT NULL COMMENT '接收人',
  `status` tinyint DEFAULT '0' COMMENT '0=pending, 1=approved, 2=rejected',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请备注',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `handle_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_from_to` (`from_user_id`,`to_user_id`),
  KEY `idx_to_user_status` (`to_user_id`,`status`),
  KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_apply`
--

LOCK TABLES `chat_apply` WRITE;
/*!40000 ALTER TABLE `chat_apply` DISABLE KEYS */;
INSERT INTO `chat_apply` (`id`, `from_user_id`, `to_user_id`, `status`, `remark`, `apply_time`, `handle_time`) VALUES (1,1,2,1,'学姐你好，可以认识一下吗？','2026-06-28 10:30:00','2026-06-28 14:20:00'),(2,1,5,0,'一起打王者吗？','2026-07-01 15:00:00',NULL),(3,1,14,2,'想和你一起看展~','2026-06-25 09:00:00','2026-06-25 18:30:00'),(4,1,18,0,'你的摄影作品好棒！','2026-07-02 08:30:00',NULL),(5,3,1,0,'一起刷LeetCode！','2026-07-01 20:00:00',NULL),(6,11,1,1,'看你动态很有趣~','2026-07-02 09:15:00','2026-07-02 16:04:09'),(7,16,1,1,'吃货求组队！','2026-06-30 12:00:00','2026-06-30 13:00:00'),(8,20,1,2,'hello~','2026-06-20 10:00:00','2026-06-20 11:30:00'),(9,3,11,0,'美女你好~','2026-07-02 07:00:00',NULL),(10,4,2,1,'学姐求带','2026-06-29 16:00:00','2026-06-29 20:00:00'),(11,5,13,0,'你弹吉他的样子好帅','2026-07-01 22:00:00',NULL),(12,12,14,1,'一起打球吗','2026-06-27 08:00:00','2026-06-27 10:00:00'),(13,13,14,2,'合唱一首？','2026-06-22 14:00:00','2026-06-22 16:00:00'),(14,15,5,0,'考研一起加油','2026-07-02 06:30:00',NULL),(15,17,16,0,'跑步完一起去吃好吃的','2026-07-01 18:00:00',NULL),(16,18,17,1,'教我拍照吧','2026-06-26 11:00:00','2026-06-26 15:00:00'),(17,21,20,0,'青岛的海很美','2026-07-02 10:00:00',NULL),(18,22,3,0,'大佬带带我','2026-07-02 10:30:00',NULL),(19,1,22,0,NULL,'2026-07-02 16:03:56',NULL),(20,1,13,0,'吉他弹得好好，想认识一下','2026-07-02 11:30:00',NULL),(21,1,16,1,'一起去找好吃的吧！','2026-06-26 18:00:00','2026-06-26 20:00:00'),(22,1,17,0,'跑步求组队！','2026-07-02 14:00:00',NULL),(23,1,20,2,'可爱的小姐姐~','2026-06-22 09:00:00','2026-06-22 12:00:00'),(24,1,21,0,'我也喜欢看书，交流一下？','2026-07-02 15:00:00',NULL),(25,1,4,1,'考研加油，一起监督','2026-06-24 07:30:00','2026-06-24 10:00:00'),(26,2,1,0,'学弟你好呀~','2026-07-02 13:00:00',NULL),(27,13,1,0,'看到你也喜欢音乐','2026-07-02 12:00:00',NULL),(28,17,1,1,'跑步搭子来了','2026-06-25 06:00:00','2026-06-25 08:00:00'),(29,18,1,0,'你喜欢拍照吗？','2026-07-01 19:00:00',NULL),(30,21,1,0,'海边的卡夫卡！我也喜欢','2026-07-02 16:00:00',NULL),(31,1,15,0,NULL,'2026-07-02 21:50:41',NULL);
/*!40000 ALTER TABLE `chat_apply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contact`
--

DROP TABLE IF EXISTS `contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `read_time` datetime DEFAULT NULL,
  `active_time` datetime DEFAULT NULL,
  `last_msg_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_uid_room` (`uid`,`room_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_uid_active` (`uid`,`active_time` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contact`
--

LOCK TABLES `contact` WRITE;
/*!40000 ALTER TABLE `contact` DISABLE KEYS */;
INSERT INTO `contact` (`id`, `uid`, `room_id`, `read_time`, `active_time`, `last_msg_id`, `created_at`, `updated_at`) VALUES (1,1,2,'2026-07-02 20:43:58','2026-06-28 15:00:00',NULL,'2026-06-28 14:20:00','2026-07-02 16:12:01'),(2,2,2,'2026-06-28 14:40:00','2026-06-28 15:00:00',NULL,'2026-06-28 14:20:00','2026-07-02 16:12:01'),(3,1,3,'2026-07-02 16:16:04','2026-06-26 21:00:00',NULL,'2026-06-26 20:00:00','2026-07-02 16:12:01'),(4,16,3,'2026-06-26 20:50:00','2026-06-26 21:00:00',NULL,'2026-06-26 20:00:00','2026-07-02 16:12:01'),(5,1,4,'2026-06-24 12:00:00','2026-06-24 12:00:00',NULL,'2026-06-24 10:00:00','2026-07-02 16:12:01'),(6,4,4,'2026-06-24 11:00:00','2026-06-24 12:00:00',NULL,'2026-06-24 10:00:00','2026-07-02 16:12:01'),(7,1,5,'2026-07-02 16:16:46','2026-06-25 10:00:00',NULL,'2026-06-25 08:00:00','2026-07-02 16:12:01'),(8,17,5,'2026-06-25 08:30:00','2026-06-25 10:00:00',NULL,'2026-06-25 08:00:00','2026-07-02 16:12:01'),(9,1,1,'2026-07-02 21:34:24','2026-07-02 20:19:12',33,'2026-07-02 09:20:00','2026-07-02 16:12:01'),(10,11,1,'2026-07-02 20:24:40','2026-07-02 20:19:12',33,'2026-07-02 09:20:00','2026-07-02 16:12:01');
/*!40000 ALTER TABLE `contact` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `conversation`
--

DROP TABLE IF EXISTS `conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user1_id` bigint NOT NULL COMMENT 'Smaller user ID',
  `user2_id` bigint NOT NULL COMMENT 'Larger user ID',
  `last_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_message_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation` (`user1_id`,`user2_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conversation`
--

LOCK TABLES `conversation` WRITE;
/*!40000 ALTER TABLE `conversation` DISABLE KEYS */;
INSERT INTO `conversation` (`id`, `user1_id`, `user2_id`, `last_message`, `last_message_at`, `created_at`) VALUES (1,1,11,'那太好了！改天一起探店吧','2026-07-02 09:28:00','2026-07-02 16:04:09'),(2,1,2,'你好','2026-07-02 16:16:13','2026-06-28 14:20:00'),(3,1,16,'那说定了，周六北门见！','2026-06-26 21:00:00','2026-06-26 20:00:00'),(4,1,4,'加油，一起上岸！','2026-06-24 12:00:00','2026-06-24 10:00:00'),(5,1,17,'明天6点操场见！','2026-06-25 10:00:00','2026-06-25 08:00:00');
/*!40000 ALTER TABLE `conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_chat`
--

DROP TABLE IF EXISTS `group_chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `member_count` int DEFAULT '1',
  `announcement` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_chat`
--

LOCK TABLES `group_chat` WRITE;
/*!40000 ALTER TABLE `group_chat` DISABLE KEYS */;
INSERT INTO `group_chat` (`id`, `name`, `avatar`, `owner_id`, `member_count`, `announcement`, `created_at`) VALUES (2,'画画',NULL,1,4,NULL,'2026-07-02 20:34:35');
/*!40000 ALTER TABLE `group_chat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_chat_member`
--

DROP TABLE IF EXISTS `group_chat_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_chat_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` tinyint DEFAULT '0' COMMENT '0=member, 1=admin, 2=owner',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_chat_member`
--

LOCK TABLES `group_chat_member` WRITE;
/*!40000 ALTER TABLE `group_chat_member` DISABLE KEYS */;
INSERT INTO `group_chat_member` (`id`, `group_id`, `user_id`, `role`, `joined_at`) VALUES (1,2,1,2,'2026-07-02 20:34:35'),(2,2,2,0,'2026-07-02 20:34:35'),(3,2,3,0,'2026-07-02 20:34:35'),(4,2,11,0,'2026-07-02 20:34:35');
/*!40000 ALTER TABLE `group_chat_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_member`
--

DROP TABLE IF EXISTS `group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `uid` bigint NOT NULL,
  `role` tinyint DEFAULT '0' COMMENT '0=成员 1=管理员 2=群主',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_uid` (`group_id`,`uid`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_member`
--

LOCK TABLES `group_member` WRITE;
/*!40000 ALTER TABLE `group_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `group_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group_message`
--

DROP TABLE IF EXISTS `group_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `from_user_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `message_type` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'text',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_group_msg` (`group_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group_message`
--

LOCK TABLES `group_message` WRITE;
/*!40000 ALTER TABLE `group_message` DISABLE KEYS */;
INSERT INTO `group_message` (`id`, `group_id`, `from_user_id`, `content`, `message_type`, `created_at`) VALUES (1,2,1,'hello','text','2026-07-02 20:34:40');
/*!40000 ALTER TABLE `group_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mate_invitation`
--

DROP TABLE IF EXISTS `mate_invitation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mate_invitation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT 'Creator',
  `category` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'travel/carpool/fitness/study/exam/sports/gaming/entertainment/other',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `activity_time` datetime DEFAULT NULL,
  `location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `max_participants` int DEFAULT '10',
  `current_participants` int DEFAULT '1',
  `is_anonymous` tinyint DEFAULT '0',
  `status` tinyint DEFAULT '1' COMMENT '0=cancelled, 1=active, 2=closed',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`,`status`),
  KEY `idx_location` (`latitude`,`longitude`),
  KEY `idx_activity_time` (`activity_time`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status_time` (`status`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mate_invitation`
--

LOCK TABLES `mate_invitation` WRITE;
/*!40000 ALTER TABLE `mate_invitation` DISABLE KEYS */;
INSERT INTO `mate_invitation` (`id`, `user_id`, `category`, `title`, `description`, `activity_time`, `location`, `latitude`, `longitude`, `max_participants`, `current_participants`, `is_anonymous`, `status`, `created_at`, `updated_at`) VALUES (1,1,'sports','东操场足球局','周末下午踢球，缺5个人，来的报名 ⚽','2026-07-05 15:00:00','东操场',NULL,NULL,10,3,0,1,'2026-07-01 20:23:32','2026-07-01 20:23:32'),(2,2,'entertainment','周六万象汇看电影','看《奥本海默》，晚上7点场，看完顺便吃饭 🎬','2026-07-04 19:00:00','万象汇万达影城',NULL,NULL,6,4,0,1,'2026-07-01 20:23:32','2026-07-01 20:23:32'),(3,3,'study','图书馆刷题搭子','每天下午2点-6点图书馆三楼，已有3人，再招2人 📚','2026-07-03 14:00:00','图书馆三楼自习室',NULL,NULL,5,3,0,1,'2026-07-01 20:23:32','2026-07-01 20:23:32'),(4,4,'exam','考研搭子互相监督','找2-3个考研的伙伴，每天打卡复习进度 💪','2026-07-02 08:00:00','逸夫图书馆',NULL,NULL,4,2,0,1,'2026-07-01 20:23:32','2026-07-01 20:23:32'),(5,5,'gaming','王者荣耀开黑','晚上组队排位，来几个靠谱的队友 🎮','2026-07-02 20:00:00','线上',NULL,NULL,5,5,0,2,'2026-07-01 20:23:32','2026-07-01 20:23:32'),(6,2,'travel','暑假泰山游','7月中旬爬泰山，找3-5个同学一起 🏔️','2026-07-15 06:00:00','泰山',NULL,NULL,6,2,0,1,'2026-07-01 20:23:32','2026-07-01 20:23:32'),(7,1,'fitness','健身房打卡','学校健身房，每周一三五晚上7点，互相监督 💪','2026-07-03 19:00:00','学校健身房',NULL,NULL,8,4,0,1,'2026-07-01 20:23:32','2026-07-01 20:23:32'),(8,1,'travel','旅游','22222','2026-07-04 02:20:00','213',NULL,NULL,6,1,0,1,'2026-07-02 08:17:42','2026-07-02 08:17:42');
/*!40000 ALTER TABLE `mate_invitation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mate_participant`
--

DROP TABLE IF EXISTS `mate_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mate_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invitation_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` tinyint DEFAULT '0' COMMENT '0=pending, 1=accepted, 2=rejected',
  `message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Applicant message',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mate_user` (`invitation_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mate_participant`
--

LOCK TABLES `mate_participant` WRITE;
/*!40000 ALTER TABLE `mate_participant` DISABLE KEYS */;
INSERT INTO `mate_participant` (`id`, `invitation_id`, `user_id`, `status`, `message`, `created_at`) VALUES (1,1,2,1,NULL,'2026-07-01 20:23:32'),(2,1,3,1,NULL,'2026-07-01 20:23:32'),(3,2,1,1,NULL,'2026-07-01 20:23:32'),(4,2,3,1,NULL,'2026-07-01 20:23:32'),(5,2,4,1,NULL,'2026-07-01 20:23:32'),(6,3,1,1,NULL,'2026-07-01 20:23:32'),(7,3,2,1,NULL,'2026-07-01 20:23:32'),(8,4,1,1,NULL,'2026-07-01 20:23:32'),(9,5,1,1,NULL,'2026-07-01 20:23:32'),(10,5,2,1,NULL,'2026-07-01 20:23:32'),(11,5,3,1,NULL,'2026-07-01 20:23:32'),(12,5,4,1,NULL,'2026-07-01 20:23:32'),(13,6,1,1,NULL,'2026-07-01 20:23:32'),(14,7,2,1,NULL,'2026-07-01 20:23:32'),(15,7,3,1,NULL,'2026-07-01 20:23:32'),(16,7,4,1,NULL,'2026-07-01 20:23:32'),(17,1,1,0,NULL,'2026-07-01 21:21:46');
/*!40000 ALTER TABLE `mate_participant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint DEFAULT NULL COMMENT 'NULL for system messages',
  `to_user_id` bigint NOT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'like/comment/follow/mate_request/mate_accepted/mate_rejected/system',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint DEFAULT NULL COMMENT 'post_id or invitation_id',
  `is_read` tinyint DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_msg_to_user` (`to_user_id`,`is_read`,`created_at`),
  KEY `idx_msg_type` (`to_user_id`,`type`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message`
--

LOCK TABLES `message` WRITE;
/*!40000 ALTER TABLE `message` DISABLE KEYS */;
INSERT INTO `message` (`id`, `from_user_id`, `to_user_id`, `type`, `content`, `related_id`, `is_read`, `created_at`) VALUES (1,2,1,'like','赞了你的动态',1,1,'2026-07-01 20:23:32'),(2,3,1,'like','赞了你的动态',1,1,'2026-07-01 20:23:32'),(3,3,1,'comment','评论了你的动态：下午6点准时！',1,1,'2026-07-01 20:23:32'),(4,1,2,'like','赞了你的动态',2,1,'2026-07-01 20:23:32'),(5,1,3,'mate_request','申请加入你的搭子邀约「图书馆刷题搭子」',3,0,'2026-07-01 20:23:32'),(6,2,1,'mate_request','申请加入你的搭子邀约「东操场足球局」',1,1,'2026-07-01 20:23:32'),(7,NULL,1,'system','欢迎加入Finding！请完成学生实名认证以使用全部功能',NULL,1,'2026-07-01 20:23:32'),(8,NULL,5,'system','欢迎加入Finding！请完成学生实名认证以使用全部功能',NULL,0,'2026-07-01 20:23:32'),(9,1,2,'like','赞了你的动态',2,0,'2026-07-01 20:35:16'),(10,1,2,'like','赞了你的动态',2,0,'2026-07-01 20:35:17'),(11,1,1,'mate_request','申请加入你的搭子邀约',1,1,'2026-07-01 21:21:46'),(12,1,5,'like','赞了你的动态',6,0,'2026-07-01 21:33:08'),(13,1,3,'like','赞了你的动态',3,0,'2026-07-01 21:33:35'),(14,1,5,'like','赞了你的动态',6,0,'2026-07-01 21:33:36'),(15,1,3,'comment','评论了你的动态',3,0,'2026-07-01 21:47:53'),(16,1,4,'comment','回复了你的评论',3,0,'2026-07-01 21:47:59'),(17,1,22,'chat_apply','sjk向你发送了聊天申请',19,0,'2026-07-02 16:03:56'),(18,1,11,'chat_approved','你的聊天申请已通过（sjk）',6,1,'2026-07-02 16:04:09'),(19,2,1,'comment','小美学姐 评论了你的动态',1,1,'2026-07-01 12:30:00'),(20,3,1,'comment','程序员小刚 评论了你的动态',1,1,'2026-07-01 13:30:00'),(21,11,1,'like','甜甜的草莓酱 赞了你的动态',1,1,'2026-07-01 15:00:00'),(22,16,1,'comment','吃货小分队 评论了你的动态',4,1,'2026-06-26 09:30:00'),(23,22,1,'comment','码农日记 评论了你的动态',5,1,'2026-07-02 00:10:00'),(24,14,1,'like','画画的小鹿 赞了你的动态',8,1,'2026-06-21 11:00:00'),(25,17,1,'chat_approved','你的聊天申请已通过（跑步达人Leo）',23,1,'2026-06-25 08:00:00'),(26,13,1,'chat_apply','音乐小王子 向你发送了聊天申请',22,1,'2026-07-02 12:00:00'),(27,18,1,'chat_apply','摄影爱好者Cc 向你发送了聊天申请',24,1,'2026-07-01 19:00:00'),(28,21,1,'chat_apply','海边的卡夫卡 向你发送了聊天申请',25,1,'2026-07-02 16:00:00'),(29,2,1,'chat_apply','小美学姐 向你发送了聊天申请',21,1,'2026-07-02 13:00:00'),(30,5,1,'like','游戏少女 赞了你的动态',7,1,'2026-07-02 01:00:00'),(31,1,15,'chat_apply','sjk向你发送了聊天申请',31,0,'2026-07-02 21:50:41');
/*!40000 ALTER TABLE `message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message_mark`
--

DROP TABLE IF EXISTS `message_mark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_mark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `msg_id` bigint NOT NULL,
  `uid` bigint NOT NULL,
  `mark_type` tinyint NOT NULL COMMENT '1=点赞 2=踩',
  `act_type` tinyint NOT NULL COMMENT '1=标记 2=取消',
  `status` tinyint DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_uid_type` (`msg_id`,`uid`,`mark_type`),
  KEY `idx_msg_id` (`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message_mark`
--

LOCK TABLES `message_mark` WRITE;
/*!40000 ALTER TABLE `message_mark` DISABLE KEYS */;
/*!40000 ALTER TABLE `message_mark` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `images` json DEFAULT NULL,
  `location` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `view_count` int DEFAULT '0',
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `share_count` int DEFAULT '0',
  `is_hot` tinyint DEFAULT '0',
  `is_top` tinyint DEFAULT '0',
  `status` tinyint DEFAULT '1' COMMENT '0=deleted, 1=active, 2=hidden',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_hot` (`is_hot`,`like_count`,`created_at`),
  KEY `idx_city` (`city`,`created_at`),
  KEY `idx_status` (`status`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post`
--

LOCK TABLES `post` WRITE;
/*!40000 ALTER TABLE `post` DISABLE KEYS */;
INSERT INTO `post` (`id`, `user_id`, `content`, `images`, `location`, `city`, `latitude`, `longitude`, `view_count`, `like_count`, `comment_count`, `share_count`, `is_hot`, `is_top`, `status`, `created_at`, `updated_at`) VALUES (1,1,'今天操场跑步5公里，有没有一起的？🏃‍♂️ 每天下午6点东操场见！',NULL,'东操场','淄博',NULL,NULL,156,13,6,0,1,0,1,'2026-07-01 20:23:32','2026-07-02 16:10:10'),(2,2,'推荐一部超好看的电影《奥本海默》，有人一起去看吗？🎬',NULL,'万达影城','淄博',NULL,NULL,207,1,1,0,1,0,1,'2026-07-01 20:23:32','2026-07-02 16:10:10'),(3,3,'LeetCode每日一题打卡 day30！找一起刷题的学习搭子 📚',NULL,'图书馆三楼','淄博',NULL,NULL,297,4,4,0,1,0,1,'2026-07-01 20:23:32','2026-07-02 16:10:10'),(4,4,'考研倒计时180天，今天复习了高数和英语，加油！💪',NULL,'自习室','淄博',NULL,NULL,98,0,2,0,0,0,1,'2026-07-01 20:23:32','2026-07-02 16:14:04'),(5,2,'周末去淄博万象汇逛街，有一起的姐妹吗？🛍️',NULL,'万象汇','淄博',NULL,NULL,67,1,3,0,0,0,1,'2026-07-01 20:23:32','2026-07-02 16:14:04'),(6,5,'王者荣耀上分，来个靠谱打野，我玩辅助 🎮',NULL,'宿舍','淄博',NULL,NULL,322,2,1,0,1,0,1,'2026-07-01 20:23:32','2026-07-02 16:14:04'),(7,3,'山东理工大学计算机学院大作业求组队，前端/后端都可以',NULL,'计算机学院','淄博',NULL,NULL,45,0,0,0,0,0,1,'2026-07-01 20:23:32','2026-07-02 16:14:04'),(8,1,'高考结束了，欢迎学弟学妹报考山东理工大学！🎓',NULL,'山东理工大学','淄博',NULL,NULL,522,11,5,0,1,0,1,'2026-07-01 20:23:32','2026-07-02 16:10:10'),(9,1,'121312321',NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,1,'2026-07-01 23:03:29','2026-07-01 23:03:29'),(10,1,'周末骑车去了文昌湖，风景真的不错！🚴 有没有喜欢骑行的朋友，下次一起啊~','[]','文昌湖','淄博',NULL,NULL,89,3,2,0,0,0,1,'2026-06-30 09:15:00','2026-07-02 16:14:04'),(11,1,'图书馆四楼靠窗位置，有一起学习的吗？📖 每天早8点到晚10点','[]','图书馆','淄博',NULL,NULL,45,3,1,0,0,0,1,'2026-06-28 08:00:00','2026-07-02 16:14:04'),(12,1,'推荐一家学校北门的火锅店，人均50吃到撑！🍲 有没有吃货一起打卡淄博美食','[]','北门美食街','淄博',NULL,NULL,134,6,3,0,1,0,1,'2026-06-25 18:30:00','2026-07-02 16:14:04'),(13,1,'深夜学习打卡，全栈开发真的太难了 😭 但还是要坚持下去！','[]',NULL,NULL,NULL,NULL,67,3,2,0,0,0,1,'2026-07-01 23:45:00','2026-07-02 16:14:04'),(14,1,'今天天气真好，东操场的日落太美了 🌇 分享一下~','[]','东操场','淄博',NULL,NULL,205,9,2,0,1,0,1,'2026-06-20 19:00:00','2026-07-02 16:14:04');
/*!40000 ALTER TABLE `post` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_comment`
--

DROP TABLE IF EXISTS `post_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL COMMENT 'NULL=top-level, references self for replies',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `like_count` int DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_post` (`post_id`,`created_at`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_comment`
--

LOCK TABLES `post_comment` WRITE;
/*!40000 ALTER TABLE `post_comment` DISABLE KEYS */;
INSERT INTO `post_comment` (`id`, `post_id`, `user_id`, `parent_id`, `content`, `like_count`, `created_at`) VALUES (1,1,2,NULL,'我！明天一起跑 💪',0,'2026-07-01 20:23:32'),(2,1,3,NULL,'几点开始？',0,'2026-07-01 20:23:32'),(3,1,1,2,'下午6点准时！',0,'2026-07-01 20:23:32'),(4,2,1,NULL,'这周末有空，一起啊',0,'2026-07-01 20:23:32'),(5,3,4,NULL,'我也是考研的，组个队吧',0,'2026-07-01 20:23:32'),(6,6,3,NULL,'我是打野，加我一个',0,'2026-07-01 20:23:32'),(7,8,2,NULL,'欢迎报考！校园很美的',0,'2026-07-01 20:23:32'),(8,8,1,NULL,'有什么问题可以问我~',0,'2026-07-01 20:23:32'),(9,3,1,NULL,'nihao',0,'2026-07-01 21:47:53'),(10,3,1,5,'你好',0,'2026-07-01 21:47:59'),(11,3,1,9,'能看见吗',0,'2026-07-01 21:48:12'),(12,1,2,NULL,'学弟加油！我也经常去东操场',0,'2026-07-01 12:30:00'),(13,1,3,NULL,'明天一起啊🏃',0,'2026-07-01 13:30:00'),(14,1,17,NULL,'5公里太强了，我只能跑3公里 😂',0,'2026-07-01 19:30:00'),(15,4,16,NULL,'哪家哪家？求推荐！',0,'2026-06-26 09:30:00'),(16,4,18,NULL,'拍照记得发图！',0,'2026-06-26 10:30:00'),(17,5,3,NULL,'全栈大佬！带带我',0,'2026-07-01 23:50:00'),(18,5,22,NULL,'我也是，前端学完学后端，头都大了',0,'2026-07-02 00:10:00'),(19,5,11,NULL,'加油加油 💪',0,'2026-07-02 07:30:00'),(20,8,2,NULL,'东操场的日落确实很美~',0,'2026-06-20 19:30:00'),(21,8,14,NULL,'画下来了！改天发给你看',0,'2026-06-21 11:30:00'),(22,8,18,NULL,'下次一起拍日落啊！',0,'2026-06-21 14:30:00'),(23,10,2,NULL,'文昌湖我也常去！',0,'2026-06-30 14:00:00'),(24,10,16,NULL,'下次一起啊',0,'2026-07-01 08:30:00'),(25,11,14,NULL,'图书馆四楼确实很安静',0,'2026-06-28 16:30:00'),(26,12,16,NULL,'是不是老张火锅？',0,'2026-06-26 12:00:00'),(27,12,2,NULL,'有空一起去试试！',0,'2026-06-27 12:30:00'),(28,12,11,NULL,'人均50真的假的？',0,'2026-06-27 15:00:00'),(29,13,22,NULL,'同感同感 😂',0,'2026-07-02 01:30:00'),(30,13,11,NULL,'加油！',0,'2026-07-02 07:30:00'),(31,14,2,NULL,'这张拍得好好看',0,'2026-06-21 09:30:00'),(32,14,14,NULL,'好有氛围感！',0,'2026-06-22 14:30:00');
/*!40000 ALTER TABLE `post_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_like`
--

DROP TABLE IF EXISTS `post_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=132 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_like`
--

LOCK TABLES `post_like` WRITE;
/*!40000 ALTER TABLE `post_like` DISABLE KEYS */;
INSERT INTO `post_like` (`id`, `post_id`, `user_id`, `created_at`) VALUES (1,1,2,'2026-07-01 20:23:32'),(2,1,3,'2026-07-01 20:23:32'),(3,1,4,'2026-07-01 20:23:32'),(5,2,3,'2026-07-01 20:23:32'),(7,3,2,'2026-07-01 20:23:32'),(8,3,4,'2026-07-01 20:23:32'),(9,3,5,'2026-07-01 20:23:32'),(16,6,4,'2026-07-01 20:23:32'),(18,8,2,'2026-07-01 20:23:32'),(19,8,3,'2026-07-01 20:23:32'),(20,8,4,'2026-07-01 20:23:32'),(21,8,5,'2026-07-01 20:23:32'),(29,8,1,'2026-07-01 21:33:30'),(30,3,1,'2026-07-01 21:33:35'),(31,6,1,'2026-07-01 21:33:36'),(70,1,5,'2026-07-01 14:00:00'),(71,1,11,'2026-07-01 15:00:00'),(72,1,12,'2026-07-01 16:00:00'),(73,1,14,'2026-07-01 17:00:00'),(74,1,16,'2026-07-01 18:00:00'),(75,1,17,'2026-07-01 19:00:00'),(76,1,18,'2026-07-01 20:00:00'),(77,1,20,'2026-07-02 08:00:00'),(78,1,21,'2026-07-02 09:00:00'),(79,1,22,'2026-07-02 10:00:00'),(83,5,5,'2026-07-01 11:00:00'),(95,8,11,'2026-06-21 09:00:00'),(96,8,13,'2026-06-21 10:00:00'),(97,8,14,'2026-06-21 11:00:00'),(98,8,16,'2026-06-21 12:00:00'),(99,8,17,'2026-06-21 13:00:00'),(100,8,18,'2026-06-21 14:00:00'),(108,10,2,'2026-06-30 12:00:00'),(109,10,3,'2026-06-30 13:00:00'),(110,10,16,'2026-07-01 08:00:00'),(111,11,2,'2026-06-28 14:00:00'),(112,11,14,'2026-06-28 16:00:00'),(113,11,4,'2026-06-29 09:00:00'),(114,12,16,'2026-06-26 10:00:00'),(115,12,11,'2026-06-26 12:00:00'),(116,12,14,'2026-06-26 18:00:00'),(117,12,18,'2026-06-27 08:00:00'),(118,12,20,'2026-06-27 10:00:00'),(119,12,2,'2026-06-27 12:00:00'),(120,13,3,'2026-07-02 00:30:00'),(121,13,22,'2026-07-02 01:00:00'),(122,13,11,'2026-07-02 07:00:00'),(123,14,2,'2026-06-21 09:00:00'),(124,14,3,'2026-06-21 12:00:00'),(125,14,11,'2026-06-21 15:00:00'),(126,14,5,'2026-06-22 08:00:00'),(127,14,13,'2026-06-22 10:00:00'),(128,14,14,'2026-06-22 14:00:00'),(129,14,16,'2026-06-23 09:00:00'),(130,14,17,'2026-06-23 12:00:00'),(131,14,18,'2026-06-23 16:00:00');
/*!40000 ALTER TABLE `post_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `private_chat`
--

DROP TABLE IF EXISTS `private_chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `private_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL DEFAULT '0' COMMENT 'deprecated, use room_id',
  `room_id` bigint DEFAULT NULL,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `message_type` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'text' COMMENT 'text / image',
  `is_read` tinyint DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_conv` (`conversation_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `private_chat`
--

LOCK TABLES `private_chat` WRITE;
/*!40000 ALTER TABLE `private_chat` DISABLE KEYS */;
INSERT INTO `private_chat` (`id`, `conversation_id`, `room_id`, `from_user_id`, `to_user_id`, `content`, `message_type`, `is_read`, `created_at`) VALUES (1,1,1,1,11,'你好呀！你的动态很有趣~','text',1,'2026-07-02 09:20:00'),(2,1,1,11,1,'谢谢！你也喜欢美食吗？','text',1,'2026-07-02 09:22:00'),(3,1,1,1,11,'对啊，淄博的好吃的我都知道 😋','text',1,'2026-07-02 09:25:00'),(4,1,1,11,1,'那太好了！改天一起探店吧','text',1,'2026-07-02 09:28:00'),(5,2,2,1,2,'学姐你好，我是大二计算机的','text',1,'2026-06-28 14:25:00'),(6,2,2,2,1,'你好呀~ 有什么需要帮忙的吗？','text',1,'2026-06-28 14:30:00'),(7,2,2,1,2,'想请教一下选课的问题~','text',1,'2026-06-28 14:35:00'),(8,2,2,2,1,'好啊，明天图书馆聊吧','text',1,'2026-06-28 14:40:00'),(9,2,2,1,2,'好的呀，明天图书馆见！','text',1,'2026-06-28 15:00:00'),(10,3,3,1,16,'听说北门新开了家烧烤店！','text',1,'2026-06-26 20:10:00'),(11,3,3,16,1,'真的吗？我超爱吃烧烤！','text',1,'2026-06-26 20:15:00'),(12,3,3,1,16,'周六中午一起去试试？','text',1,'2026-06-26 20:30:00'),(13,3,3,16,1,'好呀！我已经馋了 🤤','text',1,'2026-06-26 20:50:00'),(14,3,3,1,16,'那说定了，周六北门见！','text',1,'2026-06-26 21:00:00'),(15,5,5,17,1,'跑步搭子来了！你一般跑多少？','text',1,'2026-06-25 08:10:00'),(16,5,5,1,17,'每天5公里，你呢？','text',1,'2026-06-25 08:20:00'),(17,5,5,17,1,'我也5公里！一起跑更有动力','text',1,'2026-06-25 08:30:00'),(18,5,5,1,17,'明天6点操场见！','text',1,'2026-06-25 10:00:00'),(19,2,2,1,2,'你好','text',0,'2026-07-02 16:16:08'),(20,2,2,1,2,'你好','text',0,'2026-07-02 16:16:13'),(21,1,1,1,11,'你好','text',1,'2026-07-02 19:52:36'),(22,1,1,11,1,'你好你好','text',1,'2026-07-02 19:52:47'),(23,1,1,1,11,'你能看到消息吗','text',1,'2026-07-02 19:57:42'),(24,1,1,11,1,'看不到','text',1,'2026-07-02 19:57:51'),(25,1,1,11,1,'测试','text',1,'2026-07-02 20:00:31'),(26,1,1,1,11,'111','text',1,'2026-07-02 20:00:53'),(27,1,1,11,1,'2222','text',1,'2026-07-02 20:07:08'),(28,1,1,11,1,'你能看到吗','text',1,'2026-07-02 20:07:14'),(29,1,1,11,1,'为什么有两边','text',1,'2026-07-02 20:07:20'),(30,1,1,11,1,'你能看到吗','text',1,'2026-07-02 20:10:42'),(31,1,1,1,11,'能看到兄弟','text',1,'2026-07-02 20:10:47'),(32,1,1,1,11,'/api/v1/images/f451a5a45fcb43c09146f1c6b038e908.jpg','image',1,'2026-07-02 20:13:33'),(33,1,1,11,1,'/api/v1/images/cd19582933e34f58834f0b58ca4abb83.png','image',1,'2026-07-02 20:19:12');
/*!40000 ALTER TABLE `private_chat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recharge_record`
--

DROP TABLE IF EXISTS `recharge_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `payment_method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transaction_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint DEFAULT '0' COMMENT '0=pending, 1=success, 2=failed',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recharge_record`
--

LOCK TABLES `recharge_record` WRITE;
/*!40000 ALTER TABLE `recharge_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `recharge_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room`
--

DROP TABLE IF EXISTS `room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` tinyint NOT NULL COMMENT '1=单聊 2=群聊 3=全员群',
  `hot_flag` tinyint DEFAULT '0' COMMENT '0=普通 1=热门群',
  `active_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_msg_id` bigint DEFAULT NULL,
  `ext_json` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_active_time` (`active_time`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room`
--

LOCK TABLES `room` WRITE;
/*!40000 ALTER TABLE `room` DISABLE KEYS */;
INSERT INTO `room` (`id`, `type`, `hot_flag`, `active_time`, `last_msg_id`, `ext_json`, `created_at`, `updated_at`) VALUES (1,1,0,'2026-07-02 20:19:12',33,NULL,'2026-07-02 16:04:09','2026-07-02 16:04:09'),(2,1,0,'2026-06-28 15:00:00',NULL,NULL,'2026-06-28 14:20:00','2026-07-02 16:12:01'),(3,1,0,'2026-06-26 21:00:00',NULL,NULL,'2026-06-26 20:00:00','2026-07-02 16:12:01'),(4,1,0,'2026-06-24 12:00:00',NULL,NULL,'2026-06-24 10:00:00','2026-07-02 16:12:01'),(5,1,0,'2026-06-25 10:00:00',NULL,NULL,'2026-06-25 08:00:00','2026-07-02 16:12:01');
/*!40000 ALTER TABLE `room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_friend`
--

DROP TABLE IF EXISTS `room_friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `uid1` bigint NOT NULL,
  `uid2` bigint NOT NULL,
  `room_key` varchar(64) NOT NULL,
  `status` tinyint DEFAULT '1' COMMENT '0=拉黑 1=正常',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_key` (`room_key`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_uid1` (`uid1`),
  KEY `idx_uid2` (`uid2`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_friend`
--

LOCK TABLES `room_friend` WRITE;
/*!40000 ALTER TABLE `room_friend` DISABLE KEYS */;
INSERT INTO `room_friend` (`id`, `room_id`, `uid1`, `uid2`, `room_key`, `status`, `created_at`) VALUES (1,1,1,11,'1_11',1,'2026-07-02 16:04:09'),(2,2,1,2,'1_2',1,'2026-06-28 14:20:00'),(3,3,1,16,'1_16',1,'2026-06-26 20:00:00'),(4,4,1,4,'1_4',1,'2026-06-24 10:00:00'),(5,5,1,17,'1_17',1,'2026-06-25 08:00:00');
/*!40000 ALTER TABLE `room_friend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room_group`
--

DROP TABLE IF EXISTS `room_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `owner_uid` bigint NOT NULL,
  `announcement` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_group`
--

LOCK TABLES `room_group` WRITE;
/*!40000 ALTER TABLE `room_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `room_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `secure_invoke_record`
--

DROP TABLE IF EXISTS `secure_invoke_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `secure_invoke_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `secure_invoke_json` json NOT NULL COMMENT '请求快照参数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=待执行, 2=已失败',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `retry_times` int DEFAULT '0' COMMENT '已重试次数',
  `max_retry_times` int DEFAULT '3' COMMENT '最大重试次数',
  `fail_reason` text COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_next_retry_time` (`next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MQ本地消息表(可靠性保障)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `secure_invoke_record`
--

LOCK TABLES `secure_invoke_record` WRITE;
/*!40000 ALTER TABLE `secure_invoke_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `secure_invoke_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_announcement`
--

DROP TABLE IF EXISTS `system_announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_announcement`
--

LOCK TABLES `system_announcement` WRITE;
/*!40000 ALTER TABLE `system_announcement` DISABLE KEYS */;
INSERT INTO `system_announcement` (`id`, `title`, `content`, `created_by`, `created_at`) VALUES (1,'关于平台学生实名认证的通知','根据平台规定，所有用户需在注册后7天内完成学生实名认证。未认证用户将限制使用搭子匹配、私信等功能。请前往「我的-实名认证」页面提交认证材料。',10,'2026-07-01 20:23:32'),(2,'Finding平台正式上线啦！','经过团队的努力，Finding大学生社交平台正式上线！欢迎山东理工大学的同学们使用。如果在使用过程中遇到问题，请联系客服。',10,'2026-07-01 20:23:32');
/*!40000 ALTER TABLE `system_announcement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` tinyint DEFAULT '0' COMMENT '0=unknown, 1=male, 2=female',
  `birthday` date DEFAULT NULL,
  `school` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `student_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'user' COMMENT 'user / admin',
  `status` tinyint DEFAULT '1' COMMENT '0=banned, 1=active, 2=frozen',
  `real_name_verified` tinyint DEFAULT '0' COMMENT '0=no, 1=pending, 2=approved, 3=rejected',
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_school` (`school`),
  KEY `idx_location` (`latitude`,`longitude`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` (`id`, `username`, `password`, `phone`, `email`, `nickname`, `avatar`, `gender`, `birthday`, `school`, `student_id`, `signature`, `city`, `latitude`, `longitude`, `role`, `status`, `real_name_verified`, `last_login_at`, `created_at`, `updated_at`) VALUES (1,'13096120690','$2b$10$SxD0l6zIK49J9jFQW5Qi0OqXKUHP9LYurBBtYDp4VxstXoFSf5b6i','13096120690',NULL,'sjk','/api/v1/images/f1dfb05f843d46048ae5be6325469a37.jpg',1,NULL,'山东理工大学','2021001001','全栈开发中，喜欢骑行和摄影~','淄博',NULL,NULL,'user',1,2,'2026-07-02 21:48:20','2026-07-01 20:23:32','2026-07-02 16:02:37'),(2,'13800000002','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000002',NULL,'小美学姐',NULL,2,NULL,'山东理工大学','2023002','大三在读，喜欢看电影和旅游 ✈️','淄博',NULL,NULL,'user',1,0,'2026-07-01 20:23:32','2026-07-01 20:23:32','2026-07-02 08:57:58'),(3,'13800000003','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000003',NULL,'程序员小刚',NULL,1,NULL,'山东理工大学','2026003','计算机系，找一起刷题的学习搭子 📚','淄博',NULL,NULL,'user',1,2,'2026-07-01 20:23:32','2026-07-01 20:23:32','2026-07-02 15:49:46'),(4,'13800000004','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000004',NULL,'考研人小王',NULL,1,NULL,'山东理工大学','2021004','24考研，找个备考搭子互相监督 💪','淄博',NULL,NULL,'user',1,0,'2026-07-01 20:23:32','2026-07-01 20:23:32','2026-07-02 08:57:58'),(5,'13800000005','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000005',NULL,'游戏少女',NULL,2,NULL,'山东理工大学','2026005','又菜又爱玩，找游戏搭子 🎮','淄博',NULL,NULL,'user',1,2,'2026-07-01 20:23:32','2026-07-01 20:23:32','2026-07-02 15:49:46'),(10,'admin','$2b$10$xUoqMwQMku06a4Ya.Y8CXuK54BsikDEhnX50ZlktiacxNcusHYhZ.','13800000000',NULL,'管理员',NULL,0,NULL,'山东理工大学',NULL,NULL,NULL,NULL,NULL,'admin',1,0,'2026-07-02 19:27:55','2026-07-01 20:23:32','2026-07-02 19:17:41'),(11,'user007','$2a$10$H7f1hjr3PjSKCflHTR/yIuNx6Mx047TwPI0RwO7veT38qM3TzbIxq','13800000007',NULL,'甜甜的草莓酱','https://api.dicebear.com/7.x/avataaars/svg?seed=strawberry',2,NULL,'山东理工大学',NULL,'喜欢看电影和旅行~','淄博',NULL,NULL,'user',1,2,'2026-07-02 19:45:20','2026-07-02 15:48:39','2026-07-02 15:49:46'),(12,'user008','$2a$10$dummyhash','13800000008',NULL,'篮球少年阿杰','https://api.dicebear.com/7.x/avataaars/svg?seed=basketball',1,NULL,'山东理工大学',NULL,'爱运动的阳光男孩，球场等你！','淄博',NULL,NULL,'user',1,0,'2026-07-02 10:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(13,'user009','$2a$10$dummyhash','13800000009',NULL,'音乐小王子','https://api.dicebear.com/7.x/avataaars/svg?seed=music',1,NULL,'山东理工大学',NULL,'弹吉他是我的最爱','淄博',NULL,NULL,'user',1,2,'2026-07-01 15:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(14,'user010','$2a$10$dummyhash','13800000010',NULL,'画画的小鹿','https://api.dicebear.com/7.x/avataaars/svg?seed=deer',2,NULL,'山东理工大学',NULL,'美术生一枚，找一起看展的搭子','淄博',NULL,NULL,'user',1,2,'2026-07-02 12:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(15,'user011','$2a$10$dummyhash','13800000011',NULL,'考研必胜','https://api.dicebear.com/7.x/avataaars/svg?seed=study',1,NULL,'山东理工大学',NULL,'24考研党，求图书馆搭子','淄博',NULL,NULL,'user',1,1,'2026-07-02 15:19:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(16,'user012','$2a$10$dummyhash','13800000012',NULL,'吃货小分队','https://api.dicebear.com/7.x/avataaars/svg?seed=foodie',2,NULL,'山东理工大学',NULL,'唯美食不可辜负~','淄博',NULL,NULL,'user',1,2,'2026-07-02 09:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(17,'user013','$2a$10$dummyhash','13800000013',NULL,'跑步达人Leo','https://api.dicebear.com/7.x/avataaars/svg?seed=runner',1,NULL,'山东理工大学',NULL,'每天5公里打卡，一起跑步吧','淄博',NULL,NULL,'user',1,2,'2026-07-02 14:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(18,'user014','$2a$10$dummyhash','13800000014',NULL,'摄影爱好者Cc','https://api.dicebear.com/7.x/avataaars/svg?seed=camera',2,NULL,'山东大学',NULL,'爱拍照的文艺女孩','济南',NULL,NULL,'user',1,2,'2026-07-02 03:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(19,'user015','$2a$10$dummyhash','13800000015',NULL,'追风少年','https://api.dicebear.com/7.x/avataaars/svg?seed=wind',1,NULL,'山东大学',NULL,'风和自由，我都想要','济南',NULL,NULL,'user',1,0,'2026-06-29 15:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(20,'user016','$2a$10$dummyhash','13800000016',NULL,'小新的蜡笔','https://api.dicebear.com/7.x/avataaars/svg?seed=crayon',2,NULL,'中国海洋大学',NULL,'可爱是第一生产力','青岛',NULL,NULL,'user',1,2,'2026-07-02 11:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(21,'user017','$2a$10$dummyhash','13800000017',NULL,'海边的卡夫卡','https://api.dicebear.com/7.x/avataaars/svg?seed=kafka',1,NULL,'中国海洋大学',NULL,'读书、写作、看海','青岛',NULL,NULL,'user',1,2,'2026-07-02 07:49:46','2026-07-02 15:48:39','2026-07-02 15:49:46'),(22,'user018','$2a$10$dummyhash','13800000018',NULL,'码农日记','https://api.dicebear.com/7.x/avataaars/svg?seed=coder',1,NULL,'山东理工大学',NULL,'全栈学习中...','淄博',NULL,NULL,'user',1,2,'2026-07-02 15:04:46','2026-07-02 15:48:39','2026-07-02 15:49:46');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_follow`
--

DROP TABLE IF EXISTS `user_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL COMMENT 'Who follows',
  `followee_id` bigint NOT NULL COMMENT 'Who is followed',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`follower_id`,`followee_id`),
  KEY `idx_followee` (`followee_id`),
  KEY `idx_follower` (`follower_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_follow`
--

LOCK TABLES `user_follow` WRITE;
/*!40000 ALTER TABLE `user_follow` DISABLE KEYS */;
INSERT INTO `user_follow` (`id`, `follower_id`, `followee_id`, `created_at`) VALUES (3,2,1,'2026-07-01 20:23:32'),(4,2,4,'2026-07-01 20:23:32'),(5,3,1,'2026-07-01 20:23:32'),(6,3,4,'2026-07-01 20:23:32'),(7,4,2,'2026-07-01 20:23:32'),(8,4,3,'2026-07-01 20:23:32'),(9,5,2,'2026-07-01 20:23:32'),(12,1,2,'2026-07-01 22:05:45'),(13,1,3,'2026-07-01 22:05:46'),(14,1,11,'2026-07-02 10:00:00'),(15,1,14,'2026-06-25 10:00:00'),(16,1,16,'2026-06-26 20:30:00'),(17,1,17,'2026-06-25 08:30:00'),(18,11,1,'2026-07-02 09:30:00'),(19,14,1,'2026-06-25 12:00:00'),(20,16,1,'2026-06-26 20:50:00'),(21,17,1,'2026-06-25 08:10:00'),(22,18,1,'2026-07-01 19:30:00'),(23,21,1,'2026-07-02 16:10:00');
/*!40000 ALTER TABLE `user_follow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_verification`
--

DROP TABLE IF EXISTS `user_verification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_verification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `student_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `school` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `id_card_front` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card_back` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `student_card` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint DEFAULT '0' COMMENT '0=pending, 1=approved, 2=rejected',
  `reviewer_id` bigint DEFAULT NULL,
  `review_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_verification`
--

LOCK TABLES `user_verification` WRITE;
/*!40000 ALTER TABLE `user_verification` DISABLE KEYS */;
INSERT INTO `user_verification` (`id`, `user_id`, `real_name`, `student_id`, `school`, `id_card_front`, `id_card_back`, `student_card`, `status`, `reviewer_id`, `review_comment`, `created_at`) VALUES (1,1,'宋俊奎','2021001001','山东理工大学','/uploads/idcard/front_demo.jpg','/uploads/idcard/back_demo.jpg','/uploads/studentcard/demo.jpg',1,0,NULL,'2026-07-02 19:18:05'),(2,2,'李同学','2021001002','山东理工大学','/uploads/idcard/front_demo2.jpg','/uploads/idcard/back_demo2.jpg','/uploads/studentcard/demo2.jpg',0,NULL,NULL,'2026-07-02 19:18:05');
/*!40000 ALTER TABLE `user_verification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vip_record`
--

DROP TABLE IF EXISTS `vip_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vip_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `level` int DEFAULT '1',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `amount` decimal(10,2) DEFAULT NULL,
  `payment_method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint DEFAULT '1' COMMENT '0=expired, 1=active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vip_record`
--

LOCK TABLES `vip_record` WRITE;
/*!40000 ALTER TABLE `vip_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `vip_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'finding'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-02 22:21:23
