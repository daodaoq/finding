# Finding 大学生社交平台 — 设计手册

> **版本**: v1.0  
> **学校**: 山东理工大学  
> **定位**: 移动优先的大学生校内社交平台  
> **最后更新**: 2026-07-02

---

## 目录

1. [项目概览](#1-项目概览)
2. [技术栈](#2-技术栈)
3. [项目结构](#3-项目结构)
4. [数据库设计](#4-数据库设计)
5. [认证与授权体系](#5-认证与授权体系)
6. [学生身份认证体系](#6-学生身份认证体系)
7. [API 接口设计](#7-api-接口设计)
8. [功能模块详解](#8-功能模块详解)
9. [实时聊天系统](#9-实时聊天系统)
10. [前端页面结构](#10-前端页面结构)
11. [后台管理系统](#11-后台管理系统)
12. [业务规则与限制](#12-业务规则与限制)
13. [配置说明](#13-配置说明)
14. [部署说明](#14-部署说明)

---

## 1. 项目概览

Finding 是一款面向山东理工大学学生的移动端社交平台，支持以下核心场景：

- **首页** — 校园动态信息流（热门/最新/关注三个 Tab，支持按浏览量/点赞数/推荐排序）
- **广场/搭子** — 找搭子功能，9 大分类筛选（旅游/拼车/健身/学习/备考/运动/游戏/娱乐/其他），按时间/距离排序
- **消息** — 系统通知 + 实时私聊
- **我的** — 个人主页、数据统计、历史记录管理
- **发帖** — 发布动态 + 发布搭子邀约
- **后台管理** — 用户管理、认证审核、动态管理、轮播管理、系统公告

### 用户角色

| 角色 | 说明 |
|------|------|
| **游客** | 未登录用户，可浏览首页/广场/搭子，不可互动 |
| **未认证用户** | 已登录但未完成学生认证，仅可浏览/点赞/关注 |
| **已认证用户** | 完成学生认证，可使用全部功能 |
| **管理员** | 后台审核认证、管理内容、发布公告 |

---

## 2. 技术栈

### 后端 (finding-server)

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 应用框架 |
| MyBatis-Plus | 3.5.5 | ORM + 分页插件 |
| MySQL | 8.0 | 主数据库 |
| Redis | 7.x | 缓存 + Token 存储 |
| Spring Security | 6.x | 认证与授权 |
| JJWT | 0.12.3 | JWT 令牌生成与验证 |
| MinIO | — | 对象存储（图片） |
| Spring WebSocket | — | 实时消息推送 |
| Knife4j | 4.4.0 | API 文档 |

### 前端 (finding-web)

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18 | UI 框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建工具 |
| React Router | 6.x | 路由管理 |
| Axios | — | HTTP 请求 |
| Zustand | — | 状态管理 |

### 管理后台 (finding-admin)

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18 | UI 框架 |
| TypeScript | 5.x | 类型安全 |
| Ant Design | 5.x | UI 组件库 |
| React Router | 6.x | 路由管理 |
| Axios | — | HTTP 请求 |

---

## 3. 项目结构

```
D:\FullStack\finding\
├── finding-server/                 # Spring Boot 后端
│   └── src/main/java/com/finding/
│       ├── common/                 # Result, ResultCode, BusinessException, VerificationGuard
│       ├── config/                 # Security, MyBatisPlus, Redis, MinIO, WebSocket, CORS
│       ├── controller/             # REST 控制器
│       ├── dto/                    # 请求参数对象
│       ├── entity/                 # 数据库实体（17 张表）
│       ├── event/                  # Spring 事件（异步消息推送）
│       ├── interceptor/            # JWT 工具类（静态获取当前用户 ID）
│       ├── mapper/                 # MyBatis-Plus Mapper
│       ├── security/               # JWT 提供器 + UserPrincipal + UserDetailsService
│       ├── service/                # 业务逻辑 + 实现
│       ├── utils/                  # 工具类（RedisUtils, GeoUtils）
│       ├── vo/                     # 响应对象
│       └── websocket/              # WebSocket 服务器
│
├── finding-web/                    # React 移动端前端
│   └── src/
│       ├── api/                    # Axios 请求封装（auth/post/mate/chat/upload/...）
│       ├── components/             # 通用组件（BottomNav, PostCard, MateCard, ...）
│       ├── hooks/                  # 自定义 Hook（useRequireLogin, useWebSocket）
│       ├── layouts/                # 布局组件（MainLayout, AuthLayout）
│       ├── pages/                  # 页面组件（按功能分目录）
│       ├── router/                 # 路由配置
│       ├── store/                  # Zustand 状态管理（authStore, messageStore）
│       ├── types/                  # TypeScript 类型定义
│       └── utils/                  # 工具函数
│
├── finding-admin/                  # React 管理后台
│   └── src/
│       ├── api/                    # Axios 请求封装
│       ├── components/             # AdminLayout
│       └── pages/                  # 管理页面（Dashboard, Users, Verification, Posts, Banners, Announcements）
│
└── docs/                           # 文档
    └── design-manual.md            # 本设计手册
```

---

## 4. 数据库设计

共 17 张业务表，使用 MySQL 8.0 + InnoDB + utf8mb4。

### 4.1 核心业务表

#### user — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 用户 ID |
| username | VARCHAR(50) UNIQUE | 登录用户名（手机号） |
| password | VARCHAR(255) | BCrypt 加密密码 |
| phone | VARCHAR(20) UNIQUE | 手机号 |
| email | VARCHAR(100) | 邮箱 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(500) | 头像 URL |
| gender | TINYINT | 0=未设置, 1=男, 2=女 |
| birthday | DATE | 生日 |
| school | VARCHAR(100) | 学校 |
| student_id | VARCHAR(50) | 学号 |
| signature | VARCHAR(200) | 个性签名 |
| city | VARCHAR(50) | 城市 |
| latitude / longitude | DECIMAL(10,7) | 经纬度 |
| role | VARCHAR(20) | user / admin |
| status | TINYINT | 0=封禁, 1=正常, 2=冻结 |
| real_name_verified | TINYINT | **0=未认证, 1=审核中, 2=已认证, 3=已拒绝** |
| last_login_at | DATETIME | 最后登录时间 |
| created_at / updated_at | DATETIME | 创建/更新时间 |

#### user_verification — 认证审核记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 记录 ID |
| user_id | BIGINT FK | 申请人 ID |
| real_name | VARCHAR(50) | 真实姓名 |
| student_id | VARCHAR(50) | 学号 |
| school | VARCHAR(100) | 学校 |
| id_card_front / id_card_back | VARCHAR(500) | 身份证正反面照片 |
| student_card | VARCHAR(500) | 学生证照片 |
| status | TINYINT | 0=待审核, 1=已通过, 2=已拒绝 |
| reviewer_id | BIGINT | 审核人（管理员）ID |
| review_comment | VARCHAR(500) | 审核意见/拒绝原因 |
| created_at | DATETIME | 提交时间 |

#### post — 动态表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 动态 ID |
| user_id | BIGINT FK | 发布者 ID |
| content | TEXT | 正文内容 |
| images | JSON | 图片 URL 数组 |
| location / city | VARCHAR | 位置/城市 |
| latitude / longitude | DECIMAL(10,7) | 经纬度 |
| view_count / like_count / comment_count / share_count | INT | 反范式计数器 |
| is_hot / is_top | TINYINT | 热门标记/置顶标记 |
| status | TINYINT | 0=已删除, 1=正常, 2=隐藏 |
| created_at / updated_at | DATETIME | 创建/更新时间 |

#### post_comment — 评论表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 评论 ID |
| post_id | BIGINT FK | 所属动态 ID |
| user_id | BIGINT FK | 评论者 ID |
| parent_id | BIGINT nullable | 父评论 ID（NULL 为一级评论） |
| content | VARCHAR(1000) | 评论内容 |
| like_count | INT | 点赞数 |
| created_at | DATETIME | 评论时间 |

#### mate_invitation — 搭子邀约表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 邀约 ID |
| user_id | BIGINT FK | 发起人 ID |
| category | VARCHAR(20) | 分类（travel/carpool/fitness/study/exam/sports/gaming/entertainment/other） |
| title | VARCHAR(100) | 邀约标题 |
| description | TEXT | 详细描述 |
| activity_time | DATETIME | 活动时间 |
| location | VARCHAR(200) | 活动地点 |
| max_participants | INT | 最大参与人数 |
| current_participants | INT | 当前参与人数 |
| is_anonymous | TINYINT | 是否匿名发布 |
| status | TINYINT | 0=已取消, 1=进行中, 2=已关闭 |
| created_at / updated_at | DATETIME | 创建/更新时间 |

#### mate_participant — 搭子参与者表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 记录 ID |
| invitation_id | BIGINT FK | 邀约 ID |
| user_id | BIGINT FK | 参与者 ID |
| status | TINYINT | 0=待接受, 1=已接受, 2=已拒绝 |
| message | VARCHAR(500) | 申请留言 |
| created_at | DATETIME | 申请时间 |

### 4.2 聊天相关表（MallChat 架构）

#### conversation — 私聊会话表
| 字段 | 说明 |
|------|------|
| user1_id / user2_id | 用户对（user1 < user2，防重复） |
| last_message | 最后一条消息预览 |
| last_message_at | 最后消息时间 |

#### private_chat — 聊天消息表
| 字段 | 说明 |
|------|------|
| conversation_id | 所属会话 |
| from_user_id / to_user_id | 发送方/接收方 |
| content | 消息正文 |
| message_type | text / image |
| is_read | 已读标记 |

#### room — 聊天房间表（MallChat 架构核心）
| 字段 | 说明 |
|------|------|
| type | 1=单聊, 2=群聊, 3=全服 |
| hot_flag | 热度标记 |
| active_time | 最后活跃时间 |
| last_msg_id | 最后消息 ID |

#### room_friend — 单聊房间绑定表
| 字段 | 说明 |
|------|------|
| room_id | 房间 ID |
| uid1 / uid2 | 对应用户 |
| room_key | "uid1_uid2" 格式 |
| status | 0=屏蔽, 1=正常 |

#### contact — 用户-房间订阅表
记录每个用户在每个房间的阅读位置、活跃时间、最后消息 ID。

### 4.3 其他辅助表

| 表名 | 用途 |
|------|------|
| post_like | 动态点赞记录（唯一约束：post + user） |
| user_follow | 关注关系（唯一约束：follower + followee） |
| message | 系统通知（点赞/评论/关注/搭子申请等） |
| banner | 首页轮播图 |
| system_announcement | 系统公告 |
| group_chat | 群聊信息 |
| group_chat_member | 群聊成员 |
| vip_record | VIP 会员记录 |
| recharge_record | 充值记录 |

---

## 5. 认证与授权体系

### 5.1 整体流程

```
用户登录 → POST /auth/login → 验证密码/SMS → 返回 accessToken(2h) + refreshToken(7d)
         ↓
每次请求 → Authorization: Bearer <accessToken>
         → JwtAuthenticationFilter → 验证签名+过期 → 设置 SecurityContext
         ↓
Token过期 → POST /auth/refresh → 用 refreshToken 换新 accessToken
         ↓
退出登录 → POST /auth/logout → accessToken 加入 Redis 黑名单
```

### 5.2 JWT 令牌

| 项目 | 说明 |
|------|------|
| 签名算法 | HMAC-SHA256 |
| 访问令牌有效期 | 2 小时（`jwt.access-expiration: 7200000`） |
| 刷新令牌有效期 | 7 天（`jwt.refresh-expiration: 604800000`） |
| 载荷内容 | subject=userId, auth=权限列表(ROLE_USER/ROLE_ADMIN) |
| 密钥要求 | ≥ 256 bits（32 字节） |

### 5.3 权限规则

| 路径 | 权限 |
|------|------|
| `/api/v1/auth/login`, `/register`, `/send-code`, `/refresh` | 公开 |
| `GET /api/v1/posts/**`, `GET /api/v1/mates/**` | 公开 |
| `GET /api/v1/home/banners` | 公开 |
| `GET /api/v1/users/{id}`, `/users/search` | 公开 |
| `/api/v1/images/**`, `/uploads/**` | 公开 |
| `/ws/**` | 公开 |
| `/api/v1/admin/**` | ROLE_ADMIN |
| 其他所有接口 | 需登录 |

### 5.4 前端认证状态管理

- **Token 存储**: `localStorage.accessToken` + `localStorage.refreshToken`
- **初始化验证**: 从 localStorage 读取 token → 解码 JWT payload → 检查 `exp` 是否过期 → 过期自动清除
- **401/403 处理**: 响应拦截器检测到认证错误 → 调用 `useAuthStore.logout()` → 留在当前页（不强制跳转登录页）
- **游客模式**: 未登录可浏览首页/广场/搭子，点击受保护操作时弹出登录弹窗
- **登录弹窗**: `useRequireLogin(callback)` — 已登录直接执行，未登录弹出 LoginModal，登录成功后执行回调

---

## 6. 学生身份认证体系

### 6.1 设计理念

用户首次注册后默认处于"未认证"状态，大量功能受限。必须通过学号+姓名提交认证申请，经管理员审核通过后方可使用全部功能。此设计可有效防止：
- 校外人员注册后发布垃圾内容
- 匿名账号恶意私信骚扰
- 搭子活动中的信息不对称风险

### 6.2 认证状态机

```
       注册
        ↓
   [ 未认证 ]  ←──────────────────┐
        ↓ 提交认证                │
   [ 审核中 ]                     │
     ↙     ↘                     │
[ 已通过 ]  [ 已拒绝 ] ──→ 重新提交 ┘
  (可用全部     (可重新提交)
   功能)
```

### 6.3 认证流程

**用户端：**
```
我的 → ⚠️ 点击完成学生认证
     → 填写：真实姓名 + 学号 + 学校 + 学生证照片（选填）
     → 点击「提交认证」
     → 状态变为「审核中」，等待管理员审核
     → 审核通过 → 全部功能解锁
     → 审核拒绝 → 可查看拒绝原因，修改后重新提交
```

**管理端：**
```
后台管理 → 实名审核 → 待审核列表
        → 点击「查看材料」→ 查看姓名/学号/学校/学生证照片
        → [通过] → 用户实时获得全部权限
        → [拒绝] → 填写拒绝原因 → 用户可重新提交
```

### 6.4 认证限制规则

| 功能 | 未认证 | 审核中 | 已拒绝 | 已通过 |
|------|:---:|:---:|:---:|:---:|
| 浏览首页/广场/搭子 | ✅ | ✅ | ✅ | ✅ |
| 搜索用户 | ✅ | ✅ | ✅ | ✅ |
| 点赞动态 | ✅ | ✅ | ✅ | ✅ |
| 关注用户 | ✅ | ✅ | ✅ | ✅ |
| 编辑个人资料 | ✅ | ✅ | ✅ | ✅ |
| **发帖子** | **❌** | **❌** | **❌** | ✅ |
| **评论** | **❌** | **❌** | **❌** | ✅ |
| **创建搭子** | **❌** | **❌** | **❌** | ✅ |
| **加入搭子** | **❌** | **❌** | **❌** | ✅ |
| **发私信** | **❌** | **❌** | **❌** | ✅ |

未认证用户点击受限功能 → 弹出 Toast 提示 "请先完成学生认证，认证后即可使用全部功能"

### 6.5 后端实现

- **VerificationGuard** (`com.finding.common.VerificationGuard`): 认证状态检查组件
  - `checkVerified(userId)`: 检查用户认证状态，未通过抛出 `BusinessException`（2003/2004/2005）
  - `getStatus(userId)`: 返回认证状态枚举（UNVERIFIED/PENDING/VERIFIED/REJECTED）
- **受限接口**: 在 PostController.create、PostController.addComment、MateController.create、MateController.join、ChatController.sendMessage 的开头调用 `verificationGuard.checkVerified(userId)`
- **错误码**:
  - `2003` — 未提交认证（"请先完成学生认证"）
  - `2004` — 审核中（"认证审核中，请耐心等待"）
  - `2005` — 已拒绝（"认证未通过，请重新提交"）

---

## 7. API 接口设计

### 7.1 通用约定

- **Base URL**: `/api/v1`
- **响应格式**: `{ "code": 200, "message": "操作成功", "data": {...}, "timestamp": 1234567890 }`
- **分页格式**: `{ "records": [...], "total": 100, "page": 1, "size": 10, "hasMore": true }`
- **认证**: 需认证的接口在请求头携带 `Authorization: Bearer <accessToken>`

### 7.2 错误码体系

| 范围 | 含义 | 示例 |
|------|------|------|
| 200 | 成功 | SUCCESS |
| 1xxx | 认证错误 | UNAUTHORIZED(1001), LOGIN_FAILED(1002), TOKEN_INVALID(1003), TOKEN_EXPIRED(1004), SMS_CODE_ERROR(1005) |
| 2xxx | 用户错误 | USER_NOT_FOUND(2001), VERIFICATION_REQUIRED(2003), VERIFICATION_PENDING(2004), VERIFICATION_REJECTED(2005) |
| 3xxx | 动态错误 | POST_NOT_FOUND(3001), POST_DELETED(3002), ALREADY_LIKED(3005) |
| 4xxx | 搭子错误 | MATE_NOT_FOUND(4001), MATE_FULL(4003), ALREADY_JOINED(4004), NOT_CREATOR(4005) |
| 5xxx | 消息错误 | CHAT_LIMIT_EXCEEDED(5003) |
| 9xxx | 通用错误 | PARAM_ERROR(9001), INTERNAL_ERROR(9999) |

### 7.3 完整接口列表

#### 认证模块 `/api/v1/auth`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| POST | `/login` | 公开 | 密码/SMS 登录，返回 accessToken + refreshToken |
| POST | `/register` | 公开 | 注册新账号 |
| POST | `/send-code` | 公开 | 发送短信验证码 |
| POST | `/refresh` | 公开 | 刷新 accessToken |
| POST | `/logout` | 登录 | 退出登录（token 加入黑名单） |
| GET | `/me` | 登录 | 获取当前用户信息（含统计） |
| PUT | `/profile` | 登录 | 更新个人资料（昵称/头像/性别/学校/签名/城市） |
| POST | `/verify` | 登录 | 提交学生认证申请 |

#### 用户模块 `/api/v1/users`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| GET | `/{id}` | 公开 | 查看用户主页 |
| POST | `/{id}/follow` | 登录 | 切换关注状态（已关注→取消，未关注→关注） |
| DELETE | `/{id}/follow` | 登录 | 取消关注 |
| GET | `/{id}/followers` | 公开 | 粉丝列表（分页，含互关标记） |
| GET | `/{id}/following` | 公开 | 关注列表（分页，含互关标记） |
| GET | `/search` | 公开 | 搜索用户（按昵称/学校） |

#### 动态模块 `/api/v1/posts`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| GET | `/` | 公开 | 动态列表（tab: hot/latest/following, sortBy: views/likes/recommended） |
| GET | `/{id}` | 公开 | 动态详情（同时增加浏览量） |
| POST | `/` | **已认证** | 创建动态 |
| DELETE | `/{id}` | 登录 | 删除自己的动态 |
| POST | `/{id}/like` | 登录 | 切换点赞状态 |
| GET | `/{id}/comments` | 公开 | 评论列表（一级评论分页，含前 3 条子回复） |
| POST | `/{id}/comments` | **已认证** | 添加评论（parentId 可选，用于回复） |
| DELETE | `/{id}/comments/{commentId}` | 登录 | 删除自己的评论 |
| GET | `/my-likes` | 登录 | 我点赞过的动态 |

#### 搭子模块 `/api/v1/mates`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| GET | `/` | 公开 | 搭子列表（筛选: category/keyword，排序: activity_time ASC，过滤已过期） |
| GET | `/{id}` | 公开 | 搭子详情（含距离计算） |
| POST | `/` | **已认证** | 创建搭子邀约 |
| PUT | `/{id}` | 登录 | 修改自己的邀约 |
| DELETE | `/{id}` | 登录 | 取消自己的邀约 |
| POST | `/{id}/join` | **已认证** | 申请加入搭子 |
| DELETE | `/{id}/leave` | 登录 | 退出搭子 |
| PUT | `/{id}/participants/{participantId}` | 登录 | 通过/拒绝加入申请 |
| GET | `/my` | 登录 | 我发布的邀约 |
| GET | `/my-joined` | 登录 | 我加入的邀约 |
| GET | `/categories` | 公开 | 分类列表（9 大分类 + 图标） |

#### 聊天模块 `/api/v1/chat`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| GET | `/conversations` | 登录 | 会话列表 |
| POST | `/conversations` | 登录 | 创建/获取与某用户的会话 |
| POST | `/send` | **已认证** | 发送私信（REST 方式，也支持 WebSocket） |
| GET | `/conversations/{id}/messages` | 登录 | 消息历史（游标分页，lastId 降序） |
| PUT | `/conversations/{id}/read` | 登录 | 标记会话已读 |

#### 消息通知模块 `/api/v1/messages`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| GET | `/` | 登录 | 通知列表（按类型筛选） |
| GET | `/unread-count` | 登录 | 未读通知数 |
| PUT | `/{id}/read` | 登录 | 标记单条已读 |
| PUT | `/read-all` | 登录 | 全部已读 |
| DELETE | `/{id}` | 登录 | 删除通知 |

#### 首页模块 `/api/v1/home`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| GET | `/feed` | 登录 | 推荐用户流（排除自己+已关注，优先已认证，支持距离排序） |
| GET | `/banners` | 公开 | 活跃轮播图列表 |

#### 文件模块

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| POST | `/api/v1/upload/image` | 登录 | 上传图片到 MinIO，返回代理 URL |
| POST | `/api/v1/upload/images` | 登录 | 批量上传图片 |
| GET | `/api/v1/images/{objectName}` | 公开 | 图片代理（从 MinIO 读取并返回） |

#### 管理模块 `/api/v1/admin`

| 方法 | 路径 | 认证 | 说明 |
|------|------|:---:|------|
| GET | `/verifications` | ADMIN | 认证审核列表（分页，可按状态筛选） |
| PUT | `/verifications/{id}/approve` | ADMIN | 通过认证 |
| PUT | `/verifications/{id}/reject` | ADMIN | 拒绝认证（可选备注） |

### 7.4 WebSocket 接口

| 端点 | 认证 | 说明 |
|------|:---:|------|
| `ws://host/ws/chat?token=<jwt>` | JWT URL 参数 | 实时聊天 |
| 消息类型 `chat` | — | 私聊消息转发 |
| 消息类型 `heartbeat` | — | 心跳检测（回复 pong） |

---

## 8. 功能模块详解

### 8.1 首页（Home）

**Tab 结构：**
```
热门 Tab → 子排序：按浏览量 / 按点赞数 / 推荐
最新 Tab → 按发布时间倒序
关注 Tab → 仅显示已关注用户的动态（需登录）
```

**Guest 限制**：未登录仅显示前 5 条动态（防止爬虫大量抓取）

**缓存策略**：热门列表 Redis 缓存 3 分钟，最新列表 1 分钟

### 8.2 广场 = 搭子发现（Square/Mate）

**9 大分类**（3×3 网格）：
| ✈️ 旅游 | 🚗 拼车 | 💪 健身 |
| 📚 学习 | 📝 备考 | ⚽ 运动 |
| 🎮 游戏 | 🎬 娱乐 | 📌 其他 |

**排序规则：**
- **时间最近**: `activity_time ASC`，活动时间最近的优先，且自动过滤已过期活动
- **距离最近**: 使用 Haversine 公式计算距离（地球半径 6371km），按距离排序（需用户授权位置）

**匿名发布**：支持匿名发布搭子邀约，其他用户看不到发布者信息

**参与流程**：申请加入 → 创建者收到通知 → 创建者通过/拒绝 → 申请人收到结果通知

### 8.3 消息系统（Messages）

**两层结构：**
```
┌─ 通知中心 ───────────────────────┐
│  [未读红点] 1 条互动通知          │  ← 压缩为一行
├─────────────────────────────────┤
│  💬 私聊 ─────────────────────── │  ← 分隔线
│  会话1: "最近一条消息预览..."     │
│  会话2: "好的一会见"             │
│  ...                            │
└─────────────────────────────────┘
```

**通知类型**：点赞、评论、关注、搭子申请、搭子通过、搭子拒绝、系统消息

**进入通知列表**：自动标记所有通知为已读

### 8.4 我的（Mine）

**登录态：** 渐变粉色头部（头像 + 昵称 + 学校 + 认证标识）→ 统计数据（动态/关注/粉丝，均可点击跳转）→ 功能菜单 → 退出登录

**游客态：** 品牌展示 + 手机号登录/注册按钮

**菜单项：**
| 菜单 | 跳转 |
|------|------|
| 📝 我的动态 | /mine/posts |
| ❤️ 我的点赞 | /mine/likes |
| 👫 我的搭子 | /mine/mates?tab=following（关注/粉丝 Tab） |
| 📋 我发布的邀约 | /mine/invitations |
| 📅 我加入的搭子 | /mine/joined |

**关注/粉丝列表**：
- 互相关注 → 显示 "互相关注"
- 我关注的 → 显示 "已关注"
- 我的粉丝 → 显示 "+ 关注"（粉色按钮）

### 8.5 发帖流程

```
点击底部 ＋ → 弹出操作面板
├── 📝 发帖子 → /create-post → 文本输入(5000字符) + 位置 → 发布
└── 🤝 找搭子 → /create-mate → 选分类 + 标题 + 描述 + 时间 + 地点 + 人数 → 发布
```

未登录/未认证点击发布 → 弹出登录弹窗/认证提示

### 8.6 评论系统

- 两级嵌套评论：一级评论 + 最多 3 条子回复
- 反范式计数：post.comment_count 在查询时自动同步
- 通知：被评论者（帖子作者/父评论作者）收到通知
- 手机键盘适配：使用 Visual Viewport API，评论输入框自动跟随键盘上移

### 8.7 关注系统

- **切换式关注**：已关注→取消关注，未关注→关注（无重复关注错误）
- **互关检测**：查询反向关注关系，前端显示"互相关注"
- **统计**：followCount 和 followerCount 通过 MyBatis-Plus count 查询实时计算

---

## 9. 实时聊天系统

### 9.1 架构（MallChat 风格）

```
┌──────────────────────────────────────────────────┐
│                    前端 WebSocket                  │
│  useWebSocket.ts — 连接/重连/心跳/消息发送        │
└──────────────────┬───────────────────────────────┘
                   │ ws://host/ws/chat?token=<jwt>
┌──────────────────▼───────────────────────────────┐
│              WebSocketServer.java                 │
│  ONLINE_MAP: session → userId                     │
│  USER_CHANNELS: userId → Set<session>              │
│  支持多设备同时在线                                │
└──────────────────┬───────────────────────────────┘
                   │ Spring Event
┌──────────────────▼───────────────────────────────┐
│          MessageSendListener (@Async)             │
│  1. 更新 Room.activeTime                          │
│  2. 创建/更新 Contact（双向）                     │
│  3. 推送 WebSocket 消息给接收方                   │
└──────────────────────────────────────────────────┘
```

### 9.2 核心特性

- **连接认证**: JWT token 作为 URL 参数认证
- **心跳机制**: 每 30 秒发送 heartbeat → 服务端回复 pong
- **自动重连**: 连接断开后自动重连
- **游标分页**: 聊天历史使用 `lastId` 游标降序加载
- **多端支持**: 同一用户多个 WebSocket 连接均会收到推送
- **消息状态**: 已读/未读标记，会话级别已读更新

### 9.3 数据模型关系

```
User ─┬─ RoomFriend ─── Room ─── Contact (per-user subscription)
      │                             │
      ├─ Conversation (user1<user2) │
      │      └── PrivateChat        │
      │                             │
      ├─ RoomGroup ─── Room         │
      │      └── GroupMember        │
      │                             │
      └─ Message (notifications)    │
```

---

## 10. 前端页面结构

### 10.1 路由表

```
/                                   → MainLayout → HomePage（首页）
/square                             → MainLayout → SquarePage（广场-搭子发现）
/square/post/:id                    → MainLayout → PostDetailPage（动态详情）
/mate                               → MainLayout → MatePage（搭子页 - 同广场）
/mate/:id                           → MainLayout → MateDetailPage（搭子详情）
/messages                           → MainLayout → MessagesPage（消息）
/messages/notifications             → MainLayout → NotificationsPage（通知列表）
/messages/chat                      → MainLayout → ChatDetailPage（聊天详情）
/mine                               → MainLayout → MinePage（我的）
/mine/posts                         → MainLayout → MyPostsPage（我的动态）
/mine/likes                         → MainLayout → MyLikesPage（我的点赞）
/mine/mates                         → MainLayout → MyMatesPage（关注/粉丝）
/mine/invitations                   → MainLayout → MyInvitationsPage（我发布的邀约）
/mine/joined                        → MainLayout → MyJoinedPage（我加入的搭子）
/mine/profile                       → MainLayout → ProfileEditPage（编辑资料）
/mine/verify                        → MainLayout → VerifyPage（学生认证）
/create-post                        → MainLayout → CreatePostPage（发动态）
/create-mate                        → MainLayout → CreateMatePage（找搭子）
/login                              → AuthLayout → LoginPage（登录）
/register                           → AuthLayout → RegisterPage（注册）
*                                   → Navigate to /
```

### 10.2 核心组件

| 组件 | 说明 |
|------|------|
| BottomNav | 5 槽位底部导航（首页/广场/+号/消息/我的） |
| PostCard | 动态卡片（作者信息 + 内容 + 图片 + 互动栏） |
| MateCard | 搭子卡片（分类 + 标题 + 时间/地点/人数 + 距离） |
| SearchBar | 搜索栏组件 |
| LoginModal | 全局登录弹窗（密码/SMS 切换） |
| ConfirmDialog | 自定义确认弹窗（支持 danger 模式红色按钮） |
| CreateActionSheet | 底部操作面板（发帖子/找搭子 + 取消） |
| LoadingSkeleton | 加载骨架屏 |
| EmptyState | 空状态提示 |
| Toast | 全局 Toast 消息 |
| ChatBubble | 聊天气泡 |
| ChatInputBar | 聊天输入栏 |

### 10.3 自定义 Hook

| Hook | 说明 |
|------|------|
| useRequireLogin | 登录门控：已登录 → 执行回调；未登录 → 弹出登录弹窗 → 登录成功执行回调 |
| useWebSocket | WebSocket 连接管理：连接/重连/心跳/消息发送 |

---

## 11. 后台管理系统

### 11.1 访问方式

- **URL**: `/login`（独立地址，通常运行在另一个端口）
- **登录**: 使用管理员账号的 phone + password 登录
- **鉴权**: 登录获取 JWT → 存储为 `adminToken` → 每次请求携带 `Authorization: Bearer <token>` → 后端校验 ROLE_ADMIN

### 11.2 功能页面

| 页面 | 功能 | 状态 |
|------|------|:---:|
| 数据面板 (Dashboard) | 用户数/今日动态/搭子数/待审核数 统计卡片 | Mock 数据 |
| 用户管理 (Users) | 搜索/查看/封禁/解封 | Mock 数据 |
| **实名审核 (Verification)** | Tab 分类查看 + 通过/拒绝 + 查看材料 + 拒绝原因 | ✅ 已接入真实 API |
| 动态管理 (Posts) | 搜索/查看/隐藏/删除 | Mock 数据 |
| 轮播管理 (Banners) | 新增/编辑/删除/排序 | Mock 数据 |
| 系统公告 (Announcements) | 新增/编辑/删除 | Mock 数据 |

### 11.3 管理端布局

- 左侧折叠式侧边栏（220px），粉色主题色 `#ff6b81`
- 顶部头部栏：折叠按钮 + 面包屑导航 + 管理员标识 + 退出按钮
- 底部 Footer：Finding Admin ©2026 — 山东理工大学
- 内容区：白色圆角卡片

---

## 12. 业务规则与限制

### 12.1 身份认证限制

> 详见 [第 6 章 — 学生身份认证体系](#6-学生身份认证体系)

未认证用户（`real_name_verified != 2`）在访问发帖、评论、创建搭子、加入搭子、发送私信时会被拒绝。

### 12.2 Guest 浏览限制

- 首页动态：**最多 5 条**（超过后提示登录查看更多）
- 不可发布/评论/搭子/私信
- 可以浏览所有公开内容、搜索用户

### 12.3 内容限制

| 限制项 | 值 |
|------|------|
| 动态正文 | ≤ 5000 字符 |
| 评论内容 | ≤ 1000 字符 |
| 搭子标题 | ≤ 100 字符 |
| 搭子描述 | ≤ 2000 字符 |
| 图片上传 | ≤ 5MB，仅 JPEG/PNG/WebP |

### 12.4 搭子业务规则

- 每个用户对同一邀约只能申请一次
- 匿名发布的邀约不显示作者信息
- 活动时间已过期的邀约不显示在列表中（`activity_time >= NOW()`）
- 人数已满的邀约标记 `isFull=true`，不可申请

### 12.5 验证码规则

- 6 位随机数字
- 有效期 5 分钟
- 同一手机号 60 秒内只能发送一次
- 使用后立即失效

### 12.6 关注规则

- 不能关注自己
- 切换式：已关注 → 取消关注；未关注 → 关注
- 不会有 "已关注" 的重复错误

---

## 13. 配置说明

### 13.1 后端配置

```yaml
# application.yml 关键配置

# JWT
jwt:
  access-secret: <至少 256 bits 的密钥>
  refresh-secret: <至少 256 bits 的密钥>
  access-expiration: 7200000    # 2 小时
  refresh-expiration: 604800000  # 7 天

# MinIO
minio:
  endpoint: http://localhost:9002
  access-key: admin
  secret-key: admin123456
  bucket: finding

# 文件上传
finding:
  upload:
    max-size: 5242880   # 5MB
```

### 13.2 数据库

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/finding?useUnicode=true&characterEncoding=UTF-8&serverTimezone=GMT%2B8
    username: root
    password: 123456
```

### 13.3 Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
```

### 13.4 Redis 缓存键约定

| 键模式 | TTL | 用途 |
|--------|-----|------|
| `token:refresh:{userId}` | 7 天 | 刷新令牌 |
| `token:blacklist:{token}` | 剩余有效期 | 退出登录黑名单 |
| `sms:code:{type}:{phone}` | 5 分钟 | 短信验证码 |
| `sms:limit:{phone}` | 60 秒 | 发送频率限制 |

---

## 14. 部署说明

### 14.1 环境依赖

- JDK 17+
- MySQL 8.0+
- Redis 7.x
- MinIO（Docker）

### 14.2 Docker 启动 MinIO

```bash
docker run -d --name minio \
  -p 9002:9000 -p 9003:9001 \
  -e MINIO_ROOT_USER=admin \
  -e MINIO_ROOT_PASSWORD=admin123456 \
  minio/minio server /data --console-address ":9001"
```

### 14.3 数据库初始化

```bash
mysql -uroot -p123456 < finding-server/src/main/resources/schema.sql
mysql -uroot -p123456 < finding-server/src/main/resources/test-data.sql
```

### 14.4 启动后端

```bash
cd finding-server
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

### 14.5 启动前端

```bash
cd finding-web
npm install
npm run dev
# 前端运行在 http://localhost:5173
# Vite 自动 Proxy /api → localhost:8080
```

### 14.6 启动管理后台

```bash
cd finding-admin
npm install
npm run dev
# 管理端独立运行
```

### 14.7 测试账号

所有测试用户密码统一为 `12345678`（BCrypt 加密）：

| 手机号 | 昵称 | 角色 | 认证状态 |
|--------|------|------|:---:|
| 13800000000 | 管理员 | admin | 已认证 |
| 13800000001 | 小明同学 | user | 未认证 |
| 13800000002 | 小美学姐 | user | 未认证 |
| 13800000003 | 程序员小刚 | user | 未认证 |
| 13800000004 | 考研人小王 | user | 未认证 |
| 13800000005 | 游戏少女 | user | 未认证 |

---

> **文档维护**：本手册随代码同步更新。如有功能变更，请及时更新对应章节。
