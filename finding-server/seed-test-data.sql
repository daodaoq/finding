-- ============================================================
-- Finding 测试数据(适配测试账号 13096120690 / id=1)
-- 所有测试用户密码: 12345678
-- ============================================================
USE finding;
SET NAMES utf8mb4;

-- 1) 完善测试账号资料 + 认证
UPDATE `user` SET school='山东理工大学', city='淄博', real_name_verified=2, gender=1, status=1, role='user',
  signature='测试账号：爱运动、爱交友，欢迎大家来找我玩 🏀', avatar='https://picsum.photos/seed/finding1/200/200'
WHERE id=1 AND phone='13096120690';

-- 2) 补充 4 个测试用户 + 管理员
INSERT INTO `user` (`id`,`username`,`password`,`phone`,`nickname`,`avatar`,`gender`,`school`,`student_id`,`signature`,`city`,`real_name_verified`,`status`,`role`,`last_login_at`) VALUES
(2,'13800000002','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000002','小美学姐','https://picsum.photos/seed/finding2/200/200',2,'山东理工大学','2023002','大三在读，喜欢看电影和旅游 ✈️','淄博',2,1,'user',NOW()),
(3,'13800000003','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000003','程序员小刚','https://picsum.photos/seed/finding3/200/200',1,'山东理工大学','2026003','计算机系，找一起刷题的学习搭子 📚','淄博',1,1,'user',NOW()),
(4,'13800000004','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000004','考研人小王','https://picsum.photos/seed/finding4/200/200',1,'山东理工大学','2021004','考研冲刺，找个备考搭子互相监督 💪','淄博',2,1,'user',NOW()),
(5,'13800000005','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000005','游戏少女','https://picsum.photos/seed/finding5/200/200',2,'山东理工大学','2026005','又菜又爱玩，找游戏搭子 🎮','淄博',0,1,'user',NOW());
INSERT INTO `user` (`id`,`username`,`password`,`phone`,`nickname`,`gender`,`school`,`real_name_verified`,`status`,`role`,`last_login_at`) VALUES
(10,'admin','$2a$10$pAEl9vM2h.sOYs25PXn3sOmfRU5UA/5g59gdgApLGUkIl0jGbTetW','13800000000','管理员',0,'山东理工大学',2,1,'admin',NOW());

-- 3) 关注关系
INSERT INTO `user_follow` (`follower_id`,`followee_id`) VALUES
(1,2),(1,3),(2,1),(2,4),(3,1),(3,4),(4,2),(4,3),(5,2),(5,1);

-- 4) 动态
INSERT INTO `post` (`id`,`user_id`,`content`,`location`,`city`,`view_count`,`is_hot`,`status`) VALUES
(1,1,'今天操场跑步5公里，有没有一起的？🏃 每天下午6点东操场见！','东操场','淄博',156,1,1),
(2,2,'推荐一部超好看的电影《奥本海默》，有人一起去看吗？🎬','万达影城','淄博',203,1,1),
(3,3,'LeetCode每日一题打卡 day30！找一起刷题的学习搭子 📚','图书馆三楼','淄博',289,1,1),
(4,4,'考研倒计时180天，今天复习了高数和英语，加油！💪','自习室','淄博',98,0,1),
(5,2,'周末去淄博万象汇逛街，有一起的姐妹吗？🛍️','万象汇','淄博',67,0,1),
(6,5,'王者荣耀上分，来个靠谱打野，我玩辅助 🎮','宿舍','淄博',320,1,1),
(7,3,'山东理工大学计算机学院大作业求组队，前端/后端都可以','计算机学院','淄博',45,0,1),
(8,1,'终于放暑假啦！打算去海边玩几天，有一起的吗？🌊','山东理工大学','淄博',520,1,1);

-- 5) 点赞(与 like_count 对齐)
INSERT INTO `post_like` (`post_id`,`user_id`) VALUES
(1,2),(1,3),(1,4),
(2,1),(2,3),
(3,1),(3,2),(3,4),(3,5),
(4,1),(4,2),
(5,1),
(6,1),(6,2),(6,3),(6,4),
(7,1),
(8,2),(8,3),(8,4),(8,5);

-- 6) 评论
INSERT INTO `post_comment` (`id`,`post_id`,`user_id`,`parent_id`,`content`) VALUES
(1,1,2,NULL,'我！明天一起跑 💪'),
(2,1,3,NULL,'几点开始？'),
(3,1,1,2,'下午6点准时！'),
(4,2,1,NULL,'这周末有空，一起啊'),
(5,3,4,NULL,'我也是考研的，组个队吧'),
(6,6,3,NULL,'我是打野，加我一个'),
(7,8,2,NULL,'海边超好玩的！强烈推荐'),
(8,8,1,NULL,'有什么攻略推荐吗~');

-- 评论点赞
INSERT INTO `post_comment_like` (`comment_id`,`user_id`) VALUES
(1,3),(1,4),(2,5),(5,1),(6,2);

-- 7) 搭子邀约(活动时间改为未来)
INSERT INTO `mate_invitation` (`id`,`user_id`,`category`,`title`,`description`,`activity_time`,`location`,`max_participants`,`current_participants`,`status`) VALUES
(1,1,'sports','东操场足球局','周末下午踢球，缺5个人，来的报名 ⚽','2026-08-15 15:00:00','东操场',10,3,1),
(2,2,'entertainment','周六万象汇看电影','看《奥本海默》，晚上7点场，看完顺便吃饭 🎬','2026-08-16 19:00:00','万象汇万达影城',6,4,1),
(3,3,'study','图书馆刷题搭子','每天下午2点-6点图书馆三楼，已有3人，再招2人 📚','2026-08-14 14:00:00','图书馆三楼自习室',5,3,1),
(4,4,'exam','考研搭子互相监督','找2-3个考研的伙伴，每天打卡复习进度 💪','2026-08-20 08:00:00','逸夫图书馆',4,2,1),
(5,5,'gaming','王者荣耀开黑','晚上组队排位，来几个靠谱的队友 🎮','2026-08-10 20:00:00','线上',5,5,2),
(6,2,'travel','暑假泰山游','8月中旬爬泰山，找3-5个同学一起 🏔️','2026-08-22 06:00:00','泰山',6,2,1),
(7,1,'fitness','健身房打卡','学校健身房，每周一三五晚上7点，互相监督 💪','2026-08-17 19:00:00','学校健身房',8,4,1);

-- 搭子参与者
INSERT INTO `mate_participant` (`invitation_id`,`user_id`,`status`,`message`) VALUES
(1,2,1,'报名一个！'),(1,3,1,'我来'),(2,1,1,'算我一个'),(2,3,1,'一起'),(2,4,1,'+1'),
(3,1,1,'求带'),(3,2,1,'我也刷题'),(4,1,1,'加油'),(6,1,1,'想去！'),
(7,2,1,'一起撸铁'),(7,3,1,'周三有空'),(7,4,1,'可以');

-- 8) 消息通知
INSERT INTO `message` (`from_user_id`,`to_user_id`,`type`,`content`,`related_id`,`is_read`) VALUES
(2,1,'like','赞了你的动态',1,0),
(3,1,'like','赞了你的动态',1,0),
(3,1,'comment','评论了你的动态：几点开始？',1,0),
(1,2,'like','赞了你的动态',2,1),
(1,3,'mate_request','申请加入你的搭子邀约「图书馆刷题搭子」',3,0),
(2,1,'mate_request','申请加入你的搭子邀约「东操场足球局」',1,0),
(5,1,'like','赞了你的动态',8,0),
(NULL,1,'system','欢迎加入Finding！请完成学生实名认证以使用全部功能',NULL,0);

-- 9) Banner + 公告
INSERT INTO `banner` (`title`,`image_url`,`link_url`,`sort_order`,`is_active`) VALUES
('新生交友季','https://picsum.photos/800/300?random=1','/mate',1,1),
('搭子匹配上线','https://picsum.photos/800/300?random=2','/mate',2,1),
('实名认证指南','https://picsum.photos/800/300?random=3','/mine/verify',3,1);
INSERT INTO `system_announcement` (`title`,`content`,`created_by`) VALUES
('关于平台学生实名认证的通知','根据平台规定，所有用户需在注册后7天内完成学生实名认证。未认证用户将限制使用搭子匹配、私信等功能。',10),
('Finding平台正式上线啦！','Finding大学生社交平台正式上线！欢迎山东理工大学的同学们使用。',10);

-- 10) 聊天(room/contact/private_chat/conversation) 用户1 <-> 用户2
INSERT INTO `room` (`id`,`type`,`hot_flag`,`active_time`,`last_msg_id`) VALUES
(1,1,0,'2026-08-08 20:30:00',3);
INSERT INTO `room_friend` (`room_id`,`uid1`,`uid2`,`room_key`,`status`) VALUES
(1,1,2,'1_2',1);
INSERT INTO `contact` (`uid`,`room_id`,`read_time`,`active_time`,`last_msg_id`) VALUES
(1,1,'2026-08-08 20:30:00','2026-08-08 20:30:00',3),
(2,1,'2026-08-08 20:30:00','2026-08-08 20:30:00',3);
INSERT INTO `private_chat` (`conversation_id`,`room_id`,`from_user_id`,`to_user_id`,`content`,`message_type`,`is_read`,`created_at`) VALUES
(1,1,1,2,'你好，看了你的动态，认识一下？','text',1,'2026-08-08 20:28:00'),
(1,1,2,1,'你好呀！我也是山理工的','text',0,'2026-08-08 20:29:00'),
(1,1,2,1,'周末的足球局我也想去，求带 ⚽','text',0,'2026-08-08 20:30:00');
INSERT INTO `conversation` (`user1_id`,`user2_id`,`last_message`,`last_message_at`) VALUES
(1,2,'周末的足球局我也想去，求带 ⚽','2026-08-08 20:30:00');

-- 11) 群聊
INSERT INTO `group_chat` (`id`,`name`,`owner_id`,`member_count`,`announcement`) VALUES
(1,'山理工学习交流群',1,3,'欢迎进群，文明交流~');
INSERT INTO `group_chat_member` (`group_id`,`user_id`,`role`,`joined_at`) VALUES
(1,1,2,NOW()),(1,2,0,NOW()),(1,3,0,NOW());
INSERT INTO `group_message` (`group_id`,`from_user_id`,`content`,`message_type`,`created_at`) VALUES
(1,1,'欢迎大家加入群聊！','text','2026-08-08 20:31:00'),
(1,2,'大家好，我是小美','text','2026-08-08 20:32:00'),
(1,3,'一起学习一起进步 📚','text','2026-08-08 20:33:00');

-- 12) 鹊桥聊天申请(发给测试账号)
INSERT INTO `chat_apply` (`from_user_id`,`to_user_id`,`status`,`remark`,`apply_time`) VALUES
(5,1,0,'看了你的主页，想认识一下~','2026-08-08 20:35:00'),
(2,1,0,'运动搭子求认识','2026-08-08 20:36:00');

-- 13) 对齐计数
UPDATE `post` p SET p.like_count=(SELECT COUNT(*) FROM `post_like` l WHERE l.post_id=p.id),
  p.comment_count=(SELECT COUNT(*) FROM `post_comment` c WHERE c.post_id=p.id);

-- 14) 情感简历 + 信息互换
INSERT INTO `user_resume` (`user_id`,`gender`,`age`,`birthday`,`constellation`,`height_cm`,`weight_kg`,`campus`,`major_grade`,`hometown`,`career`,`daily_routine`,`relationship_status`,`core_bottom_line`,`mbti`,`personality_traits`,`in_love_look`,`flaws`,`worldview`,`personal_tags`,`relationship_count`,`breakup_reason`,`love_shortcoming`,`love_insight`,`love_growth`,`daily_company`,`fight_mode`,`love_expression`,`opposite_boundary`,`daily_status`,`life_habits`,`short_term_plan`,`long_term_plan`,`hobbies`,`marriage_plan`,`hard_conditions`,`soft_expectations`,`bonus_points`,`love_expectation`,`love_attitude`,`photo_album`) VALUES
(1,1,23,'2003-05-20','金牛座',178,68,'山东理工大学西校区','计算机学院 大四','山东淄博','程序员实习生','早睡早起，爱跑步','单身两年，期待长久陪伴','不能接受欺骗、冷暴力、养鱼','ENFP','阳光开朗，共情力强，有责任感，粘人','会记住你的喜好，小事上很用心','有点粘人、轻微拖延','金钱观：理性消费，但舍得为喜欢的人花钱；婚恋观：认定就是一辈子','偏爱双向奔赴、不冷暴力、爱吃醋但讲道理','2次','聚少离多','不太会处理冷暴力','沟通很重要，有话直说','改掉了冷暴力，学会主动沟通','周末一起吃饭、打球','冷战不超过一天，先低头','偶尔准备小惊喜','和异性保持距离，事事报备','上课、实习、健身、和朋友聚会','每天运动一小时','毕业前找到一份好工作','想在淄博安家','跑步、健身、做饭、看电影','毕业一年后确定关系，谈两年见家长','人品好、三观正、有上进心','温柔体贴，能一起进步','情绪价值拉满，做饭好吃，会把对方规划进人生','双向奔赴，平淡中带点浪漫','认真对待每一段感情，不轻易开始也不轻言放弃','["https://picsum.photos/seed/alb1/300/300","https://picsum.photos/seed/alb2/300/300"]'),
(2,2,22,'2004-09-12','处女座',165,50,'山东理工大学东校区','文学院 大三','山东济南','学生','规律作息，爱干净','单身一年半，期待奔结婚','不能接受暧昧不清、酗酒','INFJ','安静温柔，细心体贴，情绪稳定','会把男朋友照顾得很好','敏感，容易多想','金钱观：一起攒钱；家庭观念：以家庭为重','细节控、偏爱双向奔赴、不冷暴力','1次','性格不合','有点傲娇，生气不说','被爱是能感受到的','学会主动表达，不再一味付出','一起看电影、逛街','讲道理为主，偶尔撒娇','手写信、节日礼物','有男朋友后自动远离异性','追剧、看书、养猫','早睡早起','准备考研','想成为一名老师','追剧、看书、养猫、旅行','毕业后奔结婚，谈两三年见家长','身高175+、有责任心、情绪稳定','幽默、三观一致','温柔体贴，记住每一个纪念日','希望遇到一个双向喜欢的人','真诚永远最重要，承诺不养鱼','["https://picsum.photos/seed/alb3/300/300"]'),
(3,1,21,'2005-02-03','水瓶座',175,65,'山东理工大学西校区','计算机学院 大二','山东潍坊','计算机学生','图书馆、实验室、健身房','母胎单身，期待轻松恋爱','不能接受冷暴力','ISTP','理性冷静，动手能力强，诚实靠谱','尊重你的空间，也给你依靠','直男，不太会说情话','金钱观：AA 更舒服；婚恋观：顺其自然','坦荡、边界感强、不喜欢猜','0次','-','不太会哄人','爱是行动而不是语言','更主动表达关心','一起打游戏、刷题','从不吵架，冷静沟通','直接表达，不喜欢猜','边界感强，坦荡','图书馆、实验室、健身房','健身，编程','把毕设做好','想去大厂做开发','健身、编程、打游戏','毕业工作稳定后再考虑结婚','聪明、独立、有趣','能一起变好','理性但长情，承诺了就认真到底','缘分到了自然在一起','顺其自然，但不将就','["https://picsum.photos/seed/alb4/300/300","https://picsum.photos/seed/alb5/300/300","https://picsum.photos/seed/alb6/300/300"]'),
(4,1,24,'2002-10-30','天蝎座',180,75,'山东理工大学东校区','机械学院 大四','山东青岛','考研学生','自律，爱喝咖啡','单身半年，期待长久陪伴','不能接受欺骗和暧昧','INTJ','沉稳靠谱，目标感强，有主见','认定一个人就很专一','有时太理性','金钱观：有计划地花钱；婚恋观：认真才会开始','靠谱、专一、行动派','1次','未来规划不同','工作忙时容易忽略对方','陪伴和规划都很重要','学会平衡工作和恋爱','晚上一起自习、跑步','冷处理，需要对方哄','默默做事，不常说爱','有分寸，朋友是朋友','考研自习室、篮球场','自律，爱喝咖啡','考研上岸','读研然后进大厂','篮球、自习、喝咖啡','读研期间确定关系，毕业结婚','三观正、顾家、有主见','独立但有柔软的一面','行动派，会把对方规划进未来','一起成长，互相成就','认真才会开始，承诺一心一意','["https://picsum.photos/seed/alb7/300/300"]'),
(5,2,20,'2006-06-18','双子座',160,48,'山东理工大学西校区','艺术学院 大一','山东临沂','艺术生','熬夜冠军，但开心','母胎单身，期待甜甜的恋爱','不能接受忽冷忽热','ESFP','活泼可爱，自来熟，共情力强','陪玩陪聊，让你开心','三分热度、偶尔任性','金钱观：开心最重要；婚恋观：认真且真诚','自来熟、三分热度但很真诚','0次','-','偶尔任性','开心最重要','学会坚持，不轻易放弃','一起开黑、追星','撒娇认错，来得快去得快','彩虹屁和亲亲','玩归玩，从不越界','上课、画画、打游戏','开心就好','拿奖学金','当一名插画师','画画、打游戏、追星、旅行','先谈一段甜甜的恋爱再说','长得帅、打游戏厉害','能包容我的小脾气','永远站在你这边，情绪价值管够','甜甜的恋爱快轮到我','随时都可以开始，但不养鱼','["https://picsum.photos/seed/alb8/300/300","https://picsum.photos/seed/alb9/300/300"]');

-- 信息互换: 用户1<->用户2 已互换; 用户5->1 待处理
INSERT INTO `user_info_share` (`from_user_id`,`to_user_id`,`status`,`handled_at`) VALUES
(1,2,1,'2026-08-08 21:00:00'),
(5,1,0,NULL);
