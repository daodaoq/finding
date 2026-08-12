-- 恋爱干货内置内容(原前端硬编码 seed):迁入数据库并标记为已审核通过。
-- 幂等:按 title 判断已存在则跳过(部署每次会重跑全部迁移)。

INSERT INTO love_guide (user_id, title, subtitle, content, category, review_status)
SELECT 1, '和crush聊天不冷场的10句开场', '从日常切入，轻松开启第一次对话',
'1. 刚刷到一个很有意思的视频，一下就想到你啦\n2. 上完这节课感觉好难，你听这节课能跟上吗？\n3. 食堂今天新出的菜你吃过没，想问问好不好吃\n4. 刚刚路上看到一只超可爱小猫，分享给你看看\n5. 突然想问，你平时没课的时候一般喜欢干嘛？\n6. 你们作业多不多，我今天被作业狠狠拿捏住了\n7. 想问个小问题，你有没有推荐好听的歌？\n8. 今天天气好闷，你有没有什么解暑小妙招\n9. 刚刚看到一个梗，感觉特别适合你，哈哈\n10. 我发现一个好玩的地方，你之前去过吗？\n\n小提示：开场尽量用问句/分享生活，不要只发"在吗"，给对方接话的空间。',
'聊天技巧', 1
WHERE NOT EXISTS (SELECT 1 FROM love_guide WHERE title = '和crush聊天不冷场的10句开场');

INSERT INTO love_guide (user_id, title, subtitle, content, category, review_status)
SELECT 1, '告别一问一答，聊天延伸小技巧', '摆脱查户口式对话，越聊越投机',
'✅不要：\n你喜欢看电影吗？喜欢。就结束对话。\n\n✅正确思路：提问 + 分享自己情况，抛出新话题\n示例："你平时喜欢看电影吗？我最近刚看完一部，感觉剧情还挺有意思的"\n\n👉核心：提问之后带上自己的信息，方便对方接话。避开连环审问，多观察对方回复里的关键词，顺着关键词往下聊。',
'聊天技巧', 1
WHERE NOT EXISTS (SELECT 1 FROM love_guide WHERE title = '告别一问一答，聊天延伸小技巧');

INSERT INTO love_guide (user_id, title, subtitle, content, category, review_status)
SELECT 1, '初次校园约会完整流程', '从见面地点到聊天节奏，少一点尴尬多一点自然',
'1. 见面：优先选校园奶茶店、文创街区，公共轻松场合，不要直接去偏僻地方\n2. 时间：2-3小时最合适，不要第一次就一整天，避免聊到无话可说\n3. 聊天：多聊爱好、校园趣事，少追问沉重感情过往；没话聊可以聊聊周边景物\n4. 细节：不用刻意完美，允许沉默；照顾对方感受，尊重对方想法\n5. 结束：送到方便回去的位置，分别之后简单发一句：今天和你相处挺开心。\n\n避雷：初次约会不要肢体越界，不给对方压力。',
'约会攻略', 1
WHERE NOT EXISTS (SELECT 1 FROM love_guide WHERE title = '初次校园约会完整流程');

INSERT INTO love_guide (user_id, title, subtitle, content, category, review_status)
SELECT 1, '约会结束后如何推进关系', '做好收尾，铺垫下一次见面',
'见面结束当晚简单反馈感受，不要长篇小作文。\n\n示例："今天跟你出来玩还挺开心的"\n\n如果对方反馈积极，可以顺势埋下下次邀约："下次有空可以一起去看看那家店"。如果对方回复冷淡，不必强行纠缠，放缓节奏。',
'约会攻略', 1
WHERE NOT EXISTS (SELECT 1 FROM love_guide WHERE title = '约会结束后如何推进关系');

INSERT INTO love_guide (user_id, title, subtitle, content, category, review_status)
SELECT 1, '吵架不伤害感情的沟通方式', '对事不对人，吵架不是为分出输赢',
'1. 拒绝句式："你总是……""你从来都……"，容易激化矛盾\n2. 多用感受句式：当发生XX事的时候，我会感到XX，我希望XX\n例：你很久不回消息，我会觉得被忽略，希望忙的时候简单说一声\n3. 就事论事，不翻旧账；情绪上头可以申请暂停沟通，冷静之后再聊，禁止冷暴力消失。',
'情绪沟通', 1
WHERE NOT EXISTS (SELECT 1 FROM love_guide WHERE title = '吵架不伤害感情的沟通方式');
