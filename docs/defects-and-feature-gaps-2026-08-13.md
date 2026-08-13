# Finding 全面代码扫描与功能完整性清单

> 日期：2026-08-13
> 范围：`finding-server`（Spring Boot 多模块后端）、`finding-web`（用户 H5）、`finding-admin`（管理后台）
> 方法：4 路并行静态审计（后端安全 / 后端业务逻辑 / 前端 / 功能完整性），每条结论均经代码验证并带 `文件:行号`。

## 摘要

| 严重级别 | 含义 | 数量 |
|---|---|---|
| P0 | 安全 / 越权 / 数据泄露 / 上线级风险 | 缺陷 5 + 功能缺口 5 |
| P1 | 竞态 / 越权 / 状态错乱 / 高回归 | 后端 6 + 前端 3 |
| P2 | 健壮性 / 性能 / 边界 | 后端 9 + 前端 8 + 其他 |

---

## ⚠️ 上线/部署须知（生产 Agent 必读）

> 本次 P0-1 修复把 JWT 密钥从 `application.yml` 硬编码改为**环境变量注入**。**生产启动前必须确认**以下环境变量已设置为真实随机值，否则后端启动会直接失败：

| 环境变量 | 说明 |
|---|---|
| `JWT_ACCESS_SECRET` | 访问令牌签名密钥，生成：`openssl rand -base64 64` |
| `JWT_REFRESH_SECRET` | 刷新令牌签名密钥，生成：`openssl rand -base64 64` |

- 这两个变量名**已出现在 `deploy/.env.example` 中**，但那里给的是占位符（`your-...-please-change-this`），**不能直接用于生产**，否则仍是公开已知密钥。
- 生产 `.env` 会被 `deploy.sh`（第 78 行 `set -a; source .env`）注入到 `java -jar` 进程环境，必须在 `.env` 里填真实随机值。
- 开发环境（`dev` profile）已在 `application-dev.yml` 提供 dev-only 默认值，本地运行不受影响。
- 若部署时忘记设置，后端日志会报 `Could not resolve placeholder 'JWT_ACCESS_SECRET'`（或密钥过短），这是预期内的 fail-fast 保护，不是代码 bug。

---

## 一、P0 缺陷（代码缺陷，立即修复）

- [x] **P0-1 JWT 签名密钥硬编码公开默认值 → 可伪造任意用户/管理员令牌**
  - 位置：`finding-app/src/main/resources/application.yml:39-40`
  - 影响：`jwt.access-secret` / `jwt.refresh-secret` 是字面量；`JwtTokenProvider.getAuthentication` 从令牌 `auth` claim 直接构造权限（不回查库），`SecurityConfig` 仅靠 `hasRole("ADMIN")` 拦 admin 接口。任何读过仓库的人可伪造 `sub=任意用户` + `auth=ROLE_ADMIN` 令牌，接管任意账号 + 管理员权限。
  - 修复：密钥改环境变量注入（无默认值 fail-fast），过滤器改从数据库重载角色、不信任 claim。

- [x] **P0-2 群聊消息历史无成员鉴权**
  - 位置：`finding-module-group/.../service/GroupChatService.java:218`
  - 影响：`getMessageHistory` 只按 `groupId` 查询，无成员校验。`GET /api/v1/chat/groups/{id}/messages` 仅需登录，groupId 自增可枚举，任意登录用户可读取任意群完整聊天记录。

- [x] **P0-3 群聊发消息无成员鉴权**
  - 位置：`GroupChatService.java:160`
  - 影响：`sendMessage` 未校验发送者是否群成员，任意登录用户可向任意群灌消息并广播给全体成员。

- [x] **P0-4 登录/短信验证码可爆破**
  - 位置：`SecurityConfig.java:73-75`、`AuthServiceImpl.java`（login / sendCode / verifySmsCode）
  - 影响：login/send-code `permitAll` 且无失败计数、无尝试上限；send-code 仅按手机号 60s 限流（无 IP 维度、无验证码）→ 密码在线爆破 + 短信轰炸 + 6 位码分布式爆破。
  - 修复：账号+IP 双维度登录失败锁定；验证码校验加错误次数上限；send-code 加 IP 维度限流。

- [x] **P0-5 短信验证码明文写日志**
  - 位置：`AuthServiceImpl.java:213`（`log.info("... code={}", code)`）
  - 影响：短信登录码写入生产日志（`/var/log/finding/app.log`），能读日志者可据此接管账号。
  - 修复：日志去除 code。

---

## 二、P0 功能缺口（需单独规划/决策，非代码缺陷）

> 下列为「缺失的功能」，体量较大，需产品决策或独立迭代，不在本次代码修复范围内。

- [ ] **论坛：帖子分类 / 话题 / 标签缺失** —— `Post` 实体无 category/tag/topic 字段，信息流是纯时间线 + 全文搜索，内容组织与发现能力缺失（直接关系留存）。
- [ ] **论坛：帖子搜索太弱** —— `SearchController` 仅 `like(content, keyword)` 单字段匹配，无标签/板块/作者/时间/学校过滤、无相关度排序。
- [ ] **相亲：无「互相喜欢(match)」机制** —— 当前是单向申请→审批，无双向心动即配对（主流交友平台核心体验）。
- [ ] **相亲+后台：图片/视频内容审核缺失** —— 头像/背景/相册/动态图/聊天图/视频均无机器鉴黄涉暴，人工只在实名环节看学生证。
- [ ] **后台：无数据导出** —— 全库无 CSV/Excel 导出接口或页面。

---

## 三、P1 缺陷

### 后端

- [x] **点赞计数非原子（读-改-写）** —— `PostServiceImpl.java:232-259 / 381-415`：并发点赞 `DuplicateKeyException`→500，且 `like_count` 丢失更新。应改原子 `UPDATE ... SET like_count = like_count + 1` + 捕获唯一键冲突。
- [x] **群聊 addMembers 无权限** —— `GroupChatService.java:289`：任意用户可把任何人拉进任意群（对比 `removeMember` 有 ownerId 校验）。
- [x] **群详情泄露成员列表** —— `GroupChatService.java:134`：非成员也能拿到成员昵称/头像/角色。
- [x] **信息互换状态取「最新一条」bug** —— `InfoShareAdapter.java:21-36`：A→B 已通过、之后 B→A 被拒，最新记录变 REJECTED，导致 `profile_visible=2` 用户已互换资料被意外收回。应判定「是否存在任一方向 APPROVED」。
- [x] **自动互关并发回滚** —— `BridgeServiceImpl.java:867`：`insertFollowIfAbsent` 无并发兜底，反向申请同时审批时 `DuplicateKeyException` 把「审批+建会话+互关」整体回滚。
- [x] **信息互换反向去重缺失** —— `InfoShareServiceImpl.java:90-111`：A↔B 可同时各存一条 pending；并发同方向发起报 500。

### 前端

- [x] **WS 撤回跨会话串扰** —— `useChatSocket.ts:26-29` + `GroupChat/index.tsx:38-41`：撤回事件只按 `messageId` 匹配，忽略 `action`/`conversationId`。私聊表与群消息表 ID 各自自增必然重叠 → 群聊撤回会错标私聊消息。
- [x] **未读角标双重计数竞态** —— `Messages/index.tsx:49-55` 本地 `+1` 与 `MainLayout.tsx:93` 的 `refreshUnread()` 同时改角标，语义错位。
- [x] **切换会话时旧请求提前关 loading** —— `useChatSession.ts:59-61`：`finally` 不检查 `isCurrent()`，快速切换用户时闪现空消息列表。

### 安全（P1）

- [ ] **防批量注册可绕过** —— `AuthServiceImpl.java:166-187`：仅按客户端可控的 `X-Forwarded-For` + `X-Device-Id` 计数。**已修** IP 伪造(`clientIp` 改为优先 nginx 注入的 `X-Real-IP`);**待修** 设备指纹 `X-Device-Id` 仍客户端可控(需服务端生成 HMAC 指纹)。
- [ ] **中间件默认弱口令 + 端口全网暴露** —— `deploy/docker-compose.yml`：MySQL/Redis/MinIO/RabbitMQ 默认口令 `Finding@2026`/`admin` 且 `0.0.0.0` 绑定。

---

## 四、P2 缺陷

### 后端

- [x] 搭子报名/审批未校验 `review_status`（`MateServiceImpl.java:341 / 440`）：待审/被拒活动仍可报名。
- [x] 搭子首次报名、并发退出补位无唯一键兜底 → 偶发 500（`MateServiceImpl.java:398 / 407-436`）。
- [x] 聊天申请 `applyChat` 未惰性过期旧 pending（`BridgeServiceImpl.java:450`）：过期申请仍阻塞重发。
- [x] 建群群名先赋值后清洗（`GroupChatService.java:51`）：落库未清洗原文（存储型 XSS）；评论清洗后未判空（`PostServiceImpl.java:297`）。
- [x] 反骚扰限流是进程内内存实现（`InMemoryRateLimiter`）：多实例/重启后「1 小时限 10 次」失效，应改 Redis。
- [x] 动态列表 N+1 + 读路径写库（`PostServiceImpl.java:110`）；群列表逐群查最后消息/未读数 N+1。
- [x] 公开搜索可按手机号枚举注册用户（`SearchController.java:63`，`permitAll`）。
- [x] 登出黑名单是死代码（`JwtAuthenticationFilter` 从未读 `token:blacklist:`）。
- [x] admin `deletePost` 用硬删（`AdminPostController.java:105`），与用户端软删约定不一致。

### 前端

- [x] JWT 用 `atob` 而非 base64url 解码（`tokenStorage.ts:11`）可能误清有效 token。
- [x] `Date.now()` 作临时消息 ID 同毫秒互相覆盖（`useChatActions.ts:91`）。
- [x] 多标签页登出无 `storage` 事件同步（`authStore.ts`）。
- [ ] WebSocket token 走 URL query（前后端都命中：`chatSocket.ts:95`、`WebSocketServer.java:176`）。
- [x] Resume 表单年龄/身高/生日无边界校验（`Resume/index.tsx:135-139`）。
- [ ] 未清理的定时器（`useChatSocket.ts:34`、`ChatBubble.tsx:56`、`PostDetail/index.tsx:127`）。
- [ ] Bridge 推荐加载无竞态守卫（`Bridge/index.tsx:51-92`）。
- [ ] 静默吞异常导致 UI 停在空/加载态（admin `Announcements.tsx:31`、`ChatAudit.tsx:66` 等；web `messageStore.ts:31`）。
- [x] 并发 401 时 `logout()` 被重复调用（`request.ts:77-78`，web 端缺 `loggingOut` 锁）。

---

## 五、功能缺口（P1 / P2）

### 论坛

- [ ] 置顶/精华是「死字段」：`is_top`/`is_hot` 后台无设置接口，前端「置顶标签」不生效。
- [ ] 无收藏功能（`/posts/my-likes` 是点赞不是收藏）。
- [ ] 无帖子级可见性（仅好友/仅自己）。
- [ ] 管理员删除/下架不通知作者。
- [ ] 发帖/评论无 @提及。
- [ ] 无草稿。
- [ ] 独立热榜页、视频/GIF 动态（后端有 `uploadVideo` 但 `Post` 只有 `images`）、投票帖、置顶评论、编辑历史。

### 相亲

- [ ] 无实时在线状态（只有 `lastLoginAt` 派生）。
- [ ] 资料完整度无自我引导（有 `completeness()` 评分但用户端无「还缺 xxx」）。
- [ ] 破冰话题未嵌入会话开场。
- [ ] 拉黑后残留数据（互关/关注关系）待确认清理逻辑。
- [ ] 见面安全提醒、`VipRecord` 死表无人引用。

### 后台 / 运营

- [ ] 操作审计「有写入无查看页」（`OperationAuditService` 无审计查询页面）。
- [ ] 申诉闭环太窄（仅 post 审核拒绝，`AppealController` 硬编码 `reviewStatus==2`）。
- [ ] 看板缺质量与漏斗指标（性别比/认证率/留存/审核时效）。

---

## 六、已核对无误（供参考，非缺陷）

- 密码 BCrypt 存储；全库无 `${}` 拼接 SQL（`.inSql`/`.last` 均为 Long/int 参数）。
- 搭子名额原子条件更新防超卖；聊天申请「同方向唯一待处理」有 `uk_pending_key` 兜底 + `DuplicateKeyException` 捕获。
- 私信幂等（`uk_from_client`）；聊天 outbox 可靠投递（`ChatOutboxPublisher` 定时补发带退避/死信）。
- 私信/会话越权统一走 `requireRoomMember`；拉黑 `isBlockedEitherWay` 双向判定正确。
- `/api/v1/admin/**` 全部 `hasRole("ADMIN")` 保护。
- 上传图片有魔数 + 大小 + Content-Type 校验，返回地址仅平台代理 URL。
- 前端公告 markdown 经 DOMPurify 消毒；用户内容 React 文本转义，无 XSS。
- `useStaleGuard`/`useInfiniteList` 序号守卫实现正确。

---

## 七、最值得先做的 5 件事

1. 修群聊鉴权漏洞（P0-2/3）+ 轮换 JWT 密钥改环境变量注入（P0-1）—— 纯安全，不补不应上线。
2. 图片/视频内容审核 —— 相亲+动态双场景依赖图片，当前只有文本违禁词。
3. 帖子分类/话题标签 + 搜索增强 —— 论坛核心的「组织与发现」。
4. 双向 match 机制（或明确决策）。
5. 数据导出 + 审计查看页 + 实时在线状态。
