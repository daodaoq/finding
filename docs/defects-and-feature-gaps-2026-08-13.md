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
| `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | MySQL 口令（docker-compose 与后端共用） |
| `REDIS_PASSWORD` | Redis 口令 |
| `RABBITMQ_PASSWORD` / `RABBITMQ_USER` | RabbitMQ 口令/用户 |
| `MINIO_SECRET_KEY` / `MINIO_ACCESS_KEY` | MinIO 密钥/用户 |

- 中间件口令已从 `docker-compose.yml` 与 `application-prod.yml` 移除硬编码弱口令默认值（`Finding@2026`/`admin`），改为**强制环境变量注入**：未设置时 `docker compose up` 与后端启动都会 **fail-fast**（`required variable ... is missing` / `Could not resolve placeholder`），属预期保护。
- 中间件端口已由 `0.0.0.0` 全网暴露改为**仅绑定 `127.0.0.1`**（MySQL/Redis/RabbitMQ/MinIO 均只监听本机，后端经 `localhost` 访问）；对外仅保留 Nginx 的 `80/443`。若需跨主机访问中间件，请显式调整端口映射。
- 以上变量名已出现在 `deploy/.env.example` 中（占位符 `change-me-...`），**必须替换为真实随机值**后再复制为 `.env`。
- 生产 `.env` 会被 `deploy.sh`（第 78 行 `set -a; source .env`）注入到 `java -jar` 进程环境，必须在 `.env` 里填真实值。
- 开发环境（`dev` profile）已在 `application-dev.yml` 提供 dev-only 默认值，本地运行不受影响。

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

- [x] **论坛：帖子分类 / 标签** —— 已实现固定分类(8 类)+ 自由标签(最多 5 个)，`Post` 增 `category`/`tags` 字段与迁移；「话题」以标签承载。
- [x] **论坛：帖子搜索增强** —— 已加内容+标签检索、分类/学校/时间范围过滤；相关度排序与「单独按作者过滤」待后续。
- [x] **相亲：无「互相喜欢(match)」机制** —— 已加心动/配对：`user_like` 单向心动 + `user_match` 双向配对,互相心动即配对并站内通知;前端心动按钮+互相喜欢/谁喜欢我页(保留原单向申请为「打招呼」)。
- [x] **相亲+后台：图片/视频内容审核缺失** —— 已加图片三态审核：`ImageSafetyService` 鉴黄(阿里云)+OCR 过违禁词，`image_moderation` 记录上传者/场景/风险等级，拦截删除对象、送审进后台复核队列(`/admin/image-moderation` 放行/删除)；视频暂缓。
- [x] **后台：数据导出** —— 已加用户/动态 CSV 导出接口与后台按钮(UTF-8 BOM)。

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

- [x] **防批量注册可绕过** —— IP 伪造已修(`clientIp` 优先 nginx `X-Real-IP`);设备指纹已改服务端 SHA-256(IP|UA) 派生,不再信任客户端 `X-Device-Id`。
- [x] **中间件默认弱口令 + 端口全网暴露** —— 口令改强制环境变量注入(fail-fast)+ 端口绑定 `127.0.0.1`;后端 `application-prod.yml` 密码去默认值。

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
- [x] WebSocket token 走 URL query（前后端都命中：`chatSocket.ts:95`、`WebSocketServer.java:176`）。
- [x] Resume 表单年龄/身高/生日无边界校验（`Resume/index.tsx:135-139`）。
- [x] 未清理的定时器（`useChatSocket.ts:34`、`ChatBubble.tsx:56`、`PostDetail/index.tsx:127`）。
- [x] Bridge 推荐加载无竞态守卫（`Bridge/index.tsx:51-92`）。
- [x] 静默吞异常导致 UI 停在空/加载态（admin `Announcements.tsx:31`、`ChatAudit.tsx:66` 等；web `messageStore.ts:31`）。
- [x] 并发 401 时 `logout()` 被重复调用（`request.ts:77-78`，web 端缺 `loggingOut` 锁）。

---

## 五、功能缺口（P1 / P2）

### 论坛

- [x] 置顶/精华：后台已加设置接口（`PUT /admin/posts/{id}/flag`），列表置顶优先排序，前端展示 badge。
- [x] 收藏功能：已加收藏/取消收藏 + `我的收藏`页(区别于点赞)。
- [x] 帖子级可见性：已加 0=公开/1=仅好友/2=仅自己,列表/详情/搜索按可见性过滤(好友=双向关注)。
- [x] 管理员删除/下架通知作者：已通过系统消息通知。
- [x] 发帖/评论 @提及：已解析 `@昵称` 并通知被提及用户(前端高亮;昵称唯一才通知)。
- [x] 无草稿 → 已加服务端草稿(`post_draft` 每用户一份):保存/读取/清除接口 + 发帖页自动保存/恢复/发布后清除。
- [ ] 独立热榜页、视频/GIF 动态（后端有 `uploadVideo` 但 `Post` 只有 `images`）、投票帖、置顶评论、编辑历史。

### 相亲

- [x] 无实时在线状态（只有 `lastLoginAt` 派生）→ 已加 Redis 心跳在线状态:WS 连接/心跳/断开维护 `presence:online:*`(TTL 75s),查询接口(`GET/POST /presence/online`)+ 相识卡片「在线」标识。
- [ ] 资料完整度无自我引导（有 `completeness()` 评分但用户端无「还缺 xxx」）。
- [ ] 破冰话题未嵌入会话开场。
- [ ] 拉黑后残留数据（互关/关注关系）待确认清理逻辑。
- [ ] 见面安全提醒、`VipRecord` 死表无人引用。

### 后台 / 运营

- [x] 操作审计：已加审计日志查询接口（`GET /admin/audit-logs`）与后台查看页。
- [x] 申诉闭环：抽 `AppealService` 去硬编码——被拒(`reviewStatus=2`)或被下架(`status=2`)的动态均可申诉，单内容申诉上限 3 次；通过时同时清拒绝态+恢复下架(`status→1`)。
- [x] 看板质量/漏斗指标：新增 `GET /admin/dashboard/quality`——性别比、认证率、留存率(7日活跃+老用户留存)、审核时效(待审积压+最久待处理时长)，后台面板展示。

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
