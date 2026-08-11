# Finding — 大学生校园社交平台

面向山东理工大学学生的移动端社交平台：校园动态、找搭子、相亲交友、实时聊天，配一套完整的管理后台。

## 三个端

| 端 | 目录 | 说明 | 技术 |
|----|------|------|------|
| 用户端 | `finding-web/` | 移动优先的 H5 应用 | React 18 + TS + Vite + Zustand |
| 管理后台 | `finding-admin/` | 运营/审核/内容管理 | React 18 + TS + Ant Design |
| 服务端 | `finding-server/` | 业务后端 | Spring Boot 3 + MyBatis-Plus + MySQL + Redis |

## 核心功能

- **校园动态**：发布/编辑/删除动态、图片上传、点赞、两级评论与回复、热门/最新/关注信息流
- **找搭子**：9 大分类邀约、按时间/距离排序、报名/候补/名额防超卖、匿名发布、地点脱敏
- **相亲交友（鹊桥）**：单卡滑动推荐、自定义卡片展示、聊天申请→审批→建会话、信息互换、情感简历、相亲偏好、**反馈闭环推荐算法**
- **实时聊天**：私聊 + 群聊、WebSocket 实时推送、已读回执、聊天背景、消息撤回、聊天记录搜索
- **个人中心**：资料编辑、学生实名认证、情感简历、关注/粉丝/好友、聊天/隐私/加好友/相亲偏好设置
- **搜索**：用户、动态、搭子聚合搜索
- **管理后台**：数据面板、用户管理、实名审核、动态/评论/搭子审核、举报处理、申诉、聊天审计、违禁词、公告/横幅/反馈、群管理

## 快速开始

### 环境依赖

- JDK 17+ / Maven 3.8+
- MySQL 8.0+
- Redis 7.x
- MinIO（对象存储，Docker 可快速启动）
- Node 18+（前端）

### 1. 初始化数据库

```bash
# 建库建表（幂等）
mysql -uroot -p < deploy/init.sql
# 导入种子数据（含测试账号）
mysql -uroot -p finding < deploy/init-data-full.sql
# 测试用相亲候选数据（可选）
mysql -uroot -p finding < finding-server/seed-bridge-candidates.sql
# 增量迁移（新装库通常已含,已部署库按需执行 deploy/migrations/*.sql）
```

### 2. 启动后端

```bash
cd finding-server
# 数据库/Redis/MinIO 连接信息在 finding-app/src/main/resources/application-dev.yml
mvn spring-boot:run -pl finding-app
# 默认 http://localhost:8080
```

### 3. 启动用户端

```bash
cd finding-web
npm install
npm run dev
# 默认 http://localhost:3000,已配置 /api 与 /ws 代理到后端
```

### 4. 启动管理后台

```bash
cd finding-admin
npm install
npm run dev
# 默认 http://localhost:5174(独立端口),用管理员账号登录
```

### 测试账号

统一密码：`12345678`

| 手机号 | 昵称 | 角色 | 说明 |
|--------|------|------|------|
| 13096120690 | 测试主账号 | user | 相亲测试主账号,已认证 |
| 13800000000 | 管理员 | admin | 管理后台登录 |
| 13800000002 ~ 13800000005 | 小美学姐等 | user | 常规用户 |
| 13900000101 ~ 13900000124 | 相亲候选 | user | 推荐候选用户 |

## 项目结构

```
finding/
├── finding-server/            # Spring Boot 后端(多模块)
│   ├── finding-app/           # 启动入口/配置/上传/首页聚合
│   ├── finding-common/        # 通用:Result/ResultCode/异常/违禁词
│   ├── finding-framework/     # 全局异常/限流/WebSocket 基座
│   ├── finding-module-user/   # 用户/认证/关注/拉黑/设置/简历
│   ├── finding-module-post/   # 动态/评论/点赞
│   ├── finding-module-mate/   # 搭子活动/报名/候补
│   ├── finding-module-chat/   # 相亲/私聊/信息互换/会话
│   ├── finding-module-message/# 站内通知
│   ├── finding-module-group/  # 群聊
│   └── finding-module-*       # 其他业务模块
├── finding-web/               # 用户端 H5
├── finding-admin/             # 管理后台
├── deploy/                    # 部署:init.sql/迁移/脚本/docker-compose
└── docs/                      # 文档
    ├── design-manual.md       # 设计手册(技术实现,部分章节已过时)
    ├── product-features.md    # 产品功能文档(最新,推荐先读)
    ├── CONTEXT.md             # 领域术语表
    └── *-review.md            # 各模块技术审查
```

## 文档导航

- 📖 **`docs/product-features.md`** — 产品功能全览（新同学从这里开始）
- 📘 `docs/design-manual.md` — 架构与实现细节（部分章节滞后于当前代码）
- 📗 `CONTEXT.md` — 领域术语与业务规则
- 🔍 `docs/*-review.md` — 模块技术审查与修复记录

## 主要业务规则（速览）

- 发布动态/评论/搭子/私信需**学生实名认证**；未认证仅可浏览/点赞/关注
- 游客可浏览公开内容，互动需登录（弹登录框）
- 相亲推荐会随你的**申请/跳过历史**动态调整排序
- 信息互换需**先建立聊天关系**；拉黑后隐藏会话、不可查看已互换资料
- 详情见 `docs/product-features.md` 与 `CONTEXT.md`
