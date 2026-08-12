# Finding 三端代码质量审查报告

> 审查日期：2026-08-11  
> 范围：`finding-server`（Spring Boot 多模块后端）、`finding-web`（用户 H5）、`finding-admin`（后台管理端）  
> 方法：静态代码与工程配置审查；未改动业务代码。

## 一、结论摘要

项目已经具备可运行的三端架构，用户端近期的视觉迭代也能正常构建。不过，代码质量问题集中在四条主线：

1. **安全配置不能入库**：后端存在可用的默认密码、JWT 密钥以及本地云服务密钥。
2. **前端类型边界偏弱**：两个前端均有大量 `any` 与无类型接口响应，接口变更容易演变成线上页面异常。
3. **异步行为缺少统一约束**：静默 `catch`、未取消的请求、分散的 token 读写会造成“偶现、难复现”的数据错乱。
4. **自动化质量门禁不足**：两个前端没有 lint / 测试脚本，后端没有统一的覆盖率与静态分析门禁。

建议先完成 P0，再按模块逐步推进 P1。不要将本报告中的重构事项与正在进行的视觉改版混在同一个大提交中。

## 二、风险优先级

| 优先级 | 含义 | 建议时限 | 数量 |
| --- | --- | --- | --- |
| P0 | 安全、数据一致性或核心可用性风险 | 立即处理 | 5 项 |
| P1 | 高维护成本或较高回归概率 | 近期处理 | 7 项 |
| P2 | 一致性、可读性、长期效率优化 | 规划处理 | 5 项 |

---

## 三、后端 `finding-server` 审查

### P0-1：敏感密钥与生产默认密码已写入配置文件

**证据**

- `finding-server/finding-app/src/main/resources/application.yml:34-35`：JWT access / refresh secret 有具体默认值。
- `application.yml:51-52`、`application-dev.yml:42-43`、`application-prod.yml:46-47`：MinIO 的默认账号密码在仓库中。
- `application-dev.yml`、`application-prod.yml`：MySQL、Redis、RabbitMQ 均提供了可用默认密码。
- `application-local.yml:7`：存在实际的图像安全服务密钥。

**影响**

一旦仓库、压缩包、日志或部署镜像泄露，攻击者可能伪造 JWT，连接数据库、消息队列、对象存储或调用第三方服务。即使变量可覆盖，默认值本身仍会在遗漏环境变量时启用。

**修改建议**

1. 立即轮换全部已暴露的 JWT、数据库、Redis、RabbitMQ、MinIO、第三方云密钥。
2. 移除真实默认值；生产配置使用 `${VARIABLE:?missing}` 或启动时校验必填环境变量。
3. 提交 `application-*.example.yml`，将真实 `application-local.yml` 写入 `.gitignore`。
4. CI 加入 gitleaks / trufflehog 扫描，并阻止后续密钥提交。

### P0-2：后端缺少统一的质量门禁  ✅ 已完成

**证据**

根 `pom.xml` 是 9 个模块的聚合工程，已有 `spring-boot-starter-test` 依赖；但未发现 JaCoCo、Checkstyle、SpotBugs、PMD 或 Maven Enforcer 配置。现有测试主要位于 user / post 模块，聊天、活动、群聊、消息等高风险模块没有同等覆盖。

**影响**

多人修改跨模块事务、权限和消息推送时，无法稳定阻止空指针、N+1、权限回归和序列化契约变化。

**修改建议**

- 父 POM 统一启用 `maven-enforcer-plugin`、`maven-surefire-plugin`、JaCoCo；先设置总体最低行覆盖率 50%，再逐步提升。
- 新增 SpotBugs / Checkstyle，以 warning 起步、逐步改为阻断。
- 为聊天幂等发送、信息互换、活动报名/候补/取消、封禁权限、文件上传安全补充服务层和控制器集成测试。

### P1-1：异常吞掉与可观测性不足 ✅ 已完成

**证据**

- `finding-framework/.../WebSocketServer.java` 中有 `catch (IOException ignored)` 与宽泛 `catch (Exception e)`。
- `RequestLogInterceptor.java`、`AdminUserController.java`、`FileController.java`、`UserResumeServiceImpl.java` 等处存在 `ignored` 异常。
- 上传、图片安全、审计、WebSocket、消息 outbox 等处以 `Exception` 兜底。

**影响**

实时聊天、文件上传或审计失效时，调用方可能只看到功能没反应，服务端缺少请求 ID、用户 ID、room ID 等排查证据。

**修改建议**

- 区分“预期可忽略”与“需要告警”的异常；前者写明业务原因，后者至少 `warn/error` 记录结构化上下文。
- 建立全局异常处理，统一返回业务码和 traceId。
- 为 WebSocket 连接、消息投递、上传失败、异步消费者失败增加指标和告警。

### P1-2：核心服务类偏大，职责需要拆分 ❌ 不做（服务拆分部分）

**证据**

`ChatServiceImpl`、`BridgeServiceImpl`、`MateServiceImpl` 内同时处理鉴权、查询、状态流转、数据库写入、事件发送及异常幂等，且包含大量事务方法。

**影响**

状态机改动容易影响权限与事件投递；测试需要启动过多依赖，维护成本不断上升。

**修改建议**

- 将服务拆为 command（状态变更）、query（读模型）、policy（权限与状态规则）、publisher（事件投递）。
- 将“聊天消息写库 + outbox”“报名 + 名额变动”“信息互换审批”分别定义清晰事务边界。
- 以枚举/状态迁移表替代散落的数值状态判断，并为非法迁移加测试。

**完成情况（2026-08-11）**：已落地「枚举/状态迁移表 + 非法迁移测试」——新增 `InfoShareStatus` 枚举（替换 InfoShare 服务与两个 Listener 的裸魔数，`handleShare` 增加迁移校验）；`ChatApplyStatus`/`MateInvitationStatus`/`MateParticipantStatus` 统一提供 `canTransitTo` 迁移表；新增 14 例迁移测试。**command/query/policy 服务拆分已决定不做**（体量大、现有覆盖薄、维护收益未达预期，暂缓）。

### P1-3：事务后事件的可靠性需要验收 ✅ 已完成

**证据**

存在 `@TransactionalEventListener(AFTER_COMMIT)`、消息 outbox 和 AMQP 消费者，说明已经在处理可靠投递；但需要进一步确认“写库成功、发布失败、重试、重复消费”全链路。

**修改建议**

- 为 outbox 记录增加状态、重试次数、下一次重试时间和死信处理。
- 消费端按业务唯一键实现幂等，重复消息必须安全返回。
- 以集成测试覆盖数据库提交、broker 暂时不可用、重复投递、消费者崩溃恢复。

### P2-1：开发辅助代码与配置分层 ✅ 已完成

`finding-app/src/test/java/.../GenHash.java` 使用 `System.out.println`。建议将一次性工具迁移至 `tools/` 并写明用途，测试目录只保留可自动执行的测试。

---

## 四、用户端 `finding-web` 审查

### P0-3：核心接口使用 `any`，类型契约未闭合  ✅ 已完成

**证据**

- `src/api/chat.ts:22,26,44`：消息发送、历史、搜索返回 `any`。
- `src/pages/Chat/index.tsx`：消息转换使用 `toMsg(r: any)`。
- `UserProfile`、`ChatSettings`、`Search`、`CreateGroup`、`messageStore` 等页面或 store 均存在 `any`。

**影响**

聊天、用户资料和搜索是高频页面；后端字段名称或 null 语义变化时，TypeScript 无法保护 UI。

**修改建议**

1. 在 `types/` 新增后端 DTO 与前端展示模型：`ChatMessageDTO`、`ChatMessage`、`UserProfileDTO`、`SearchResponseDTO`。
2. API 层完成 DTO 到展示模型转换；页面禁止消费 `any`。
3. 将 `catch (e: any)` 改为 `catch (e: unknown)`，集中用 `getErrorMessage()` 解析错误。

### P0-4：静默失败过多，页面数据可能悄然失真  ✅ 已完成

**证据**

`MainLayout`、`Bridge`、`Chat`、`GroupChat`、`UserProfile`、`Messages`、`Notifications` 等多处含 `.catch(() => {})` 或 `catch { /* 忽略 */ }`。

**影响**

用户无法区分“没有数据”和“请求失败”；角标、聊天设置、关注状态、消息已读状态可能不同步。

**修改建议**

- 定义请求等级：核心加载显示 error state + 重试；后台刷新保留旧值并记录日志；用户取消类错误才静默。
- 复用 `AsyncState` / `PageState` 组件统一 loading、empty、error、retry。
- Axios 拦截器返回统一 `AppError`，携带 status、业务 code、可展示文案和是否可重试。

### P0-5：认证令牌存取分散且刷新链路不统一  ✅ 已完成

**证据**

token 被 `api/request.ts`、`authStore.ts`、`Login.tsx`、`LoginModal.tsx`、`Mine/Account`、`chatSocket.ts` 直接读写；刷新令牌使用原生 `fetch`，主 API 使用 Axios。

**影响**

登出、刷新、WebSocket 重连或多标签页场景容易遗漏清理；认证逻辑难以审计。

**修改建议**

- 创建唯一的 `tokenStorage`，提供 `get/set/clear/subscribe`。
- 创建不带刷新拦截器的 `authClient`，避免刷新请求递归重试。
- Axios 重试标记 `_retry`；刷新失败时只执行一次集中登出。
- 评估将 refresh token 改为 `HttpOnly + Secure + SameSite` Cookie，降低 XSS 窃取风险。

### P1-4：大型页面组件混合了状态、请求和 UI ✅ 已完成

**证据**

`Chat/index.tsx` 432 行；聊天设置 279 行；资料编辑、认证、活动详情、简历、帖子详情、群聊、相识主页均超过 220 行或接近该规模。

**修改建议**

以聊天页为第一批重构对象：

- `useChatSession`：加载会话、历史、已读；
- `useChatSocket`：订阅、重连、去重、输入态；
- `useChatActions`：发送、重试、撤回、信息互换；
- `ChatTimeline`、`ChatComposer`、`ChatHeader`：只接收 typed props。

同样的拆分模式再推广到活动详情和资料编辑。

### P1-5：Effect 依赖和过期请求管理不一致 ✅ 已完成

**证据**

多个页面通过 `eslint-disable-next-line react-hooks/exhaustive-deps` 忽略依赖；路由参数、登录态和定位变化均可触发并发请求。

**影响**

快速切换用户、聊天对象或列表条件时，较旧请求可能覆盖最新状态。

**修改建议**

- 以 `useCallback` 或入参化 loader 满足依赖规则。
- Axios 请求接入 `AbortController` signal，卸载或依赖变化时取消。
- 为搜索、聊天、用户资料等采用 request sequence，只有最新请求可以 `setState`。

### P1-6：富文本安全策略要显式测试 ✅ 已完成

`AnnouncementModal.tsx` 使用 `dangerouslySetInnerHTML`。项目已引入 DOMPurify，但必须确认 `renderMarkdown()` 的最终 HTML 一定经过 sanitize。

建议使用 URL 协议、标签和属性白名单，并对脚本标签、事件属性、`javascript:` URL、SVG payload 写自动化测试。

### P2-2：样式令牌与格式需要治理 ✅ 已完成

部分 CSS 压缩为单行，颜色、阴影、圆角仍散落在组件中。这会增加后续统一主题和排查视觉回归的成本。

建议先自动格式化 CSS；扩展 `--surface-muted`、`--text-secondary`、`--border-subtle`、`--shadow-card`、`--love-soft` 等设计令牌，组件只使用令牌。

### P2-3：补齐前端质量工具 🕒 部分完成

用户端 `package.json` 只有 dev / build / preview。建议加入 ESLint、react-hooks 规则、Prettier、Vitest 与 Playwright，并在 CI 执行 `lint`、`build`、单元测试和关键路径冒烟测试。

**完成情况（2026-08-12）**：已加入 Prettier（含 CSS 格式化）、Vitest（sanitize 单测）、Playwright（8 例冒烟，真机全绿）。**未做**：ESLint + react-hooks 规则、CI 流水线（需接入现有 CI 后补）。

---

## 五、管理端 `finding-admin` 审查

### P0-6：后台路由没有前端访问控制  ✅ 已完成

**证据**

`src/App.tsx` 将后台所有页面直接放在 `AdminLayout` 下；未见受保护路由组件。`AdminLayout` 虽读取 token，但路由层未明确阻断未登录访问。

**影响**

未登录用户可能先进入后台页面，再因请求失败跳转；页面短暂暴露、错误体验和权限边界均不清晰。前端保护不能代替后端鉴权，但仍是必要的交互层防线。

**修改建议**

- 创建 `RequireAdminAuth`：无 token 直接 `Navigate` 到 `/login`，保存原目标地址。
- 登录成功后回跳原地址；登出清理 store、请求缓存和 token。
- 后端每个 admin API 继续作为最终 RBAC 鉴权来源。

### P1-7：管理端同样存在无类型 API 响应与表格数据 ✅ 已完成

**证据**

`ChatAudit.tsx` 的用户、群组、列定义均是 `any`；`Users.tsx` 上传和编辑 payload 使用 `any`；`ResumeEditModal.tsx` 使用 `Record<string, any>`。

**影响**

管理端直接修改用户、审核内容、聊天审计和敏感词，字段错配风险比用户端更高。

**修改建议**

- 先为 User、Post、Mate、Report、ChatAudit、Banner、Announcement 建立管理端 DTO。
- Ant Design 的 `Table`、`Form`、Upload 使用泛型，禁止业务模块新增裸 `any`。
- 审核提交 payload 定义为专门 command 类型，避免把整个表单对象直接提交。

### P1-8：后台鉴权与跳转逻辑分散 ✅ 已完成

**证据**

`api/request.ts`、`AdminLayout.tsx`、`Login.tsx` 都直接访问 `localStorage.adminToken`；过期后用 `window.location.href` 跳转。

**修改建议**

- 与用户端一致建立 `adminTokenStorage` 或抽象通用 token storage。
- 将“清 token + 提示 + 跳转”收敛为一个 logout handler。
- 在 React Router 内用 `navigate` / `Navigate` 处理跳转，降低整页刷新和重复错误提示。

### P2-4：管理端也缺少 lint、测试和 E2E 脚本 🕒 部分完成

`finding-admin/package.json` 仅提供 dev/build/preview。建议加入 lint、类型检查、组件测试；优先增加“登录拦截、审核操作、删除/封禁确认、公告发布、敏感词规则”端到端测试。

**完成情况（2026-08-12）**：已通过用户端 Playwright 冒烟覆盖管理端“登录拦截 / 管理员登录 / 用户表格加载”三条路径。**未做**：admin 自身的 lint/组件测试脚本、其余审核操作 E2E。

### P2-5：全局主题与用户端设计令牌需解耦 ✅ 已完成

管理端使用 Ant Design 的 `colorPrimary: '#ff6b81'`，用户端当前为纯白黑灰与局部淡粉的主题。两者应各自定义主题 token，不应依赖复制粘贴的颜色值；后台以信息密度和状态颜色清晰为优先。

**完成情况（2026-08-12）**：admin 在 App.tsx 定义独立语义主题（colorPrimary 品牌色 / Success / Warning / Error / Info + 小圆角），18 个文件 40+ 处硬编码色值全部改用 `theme.useToken()` 消费，与用户端设计令牌彻底解耦；仅登录页渐变装饰色保留并注明。

---

## 六、建议执行计划

### 第一阶段：安全与质量门禁（P0）

1. 轮换后端全部暴露密钥，移除默认密码，增加 secret 扫描。
2. 两前端新增 ESLint、Prettier、`lint` 脚本；后端加 Surefire、JaCoCo 和基础静态检查。
3. 用户端先消除聊天、用户资料、搜索、聊天设置 API 的 `any`。
4. 管理端先增加 `RequireAdminAuth`，再类型化审核、用户与聊天审计接口。
5. 建立统一错误对象与页面错误态，替代无理由静默 catch。

### 第二阶段：稳定性与模块边界（P1）

1. 收口用户端和管理端 token storage / 退出流程。
2. 聊天模块拆 hooks，加入请求取消、最新请求保护、WebSocket 去重测试。
3. 后端将聊天、相识、搭子服务分离 command/query/policy，补状态迁移测试。
4. 验收 outbox、消息幂等和事务后事件的失败重试。

### 第三阶段：维护效率（P2）

1. 统一 CSS 格式与设计令牌。
2. 抽取列表加载、空状态、确认动作等共用能力。
3. 建立 Playwright 冒烟集：登录、发帖、报名、聊天、信息互换、后台审核。

## 七、验收标准

- 仓库及 Git 历史扫描不再出现真实密钥、密码或可用默认凭据。
- 三端 CI 均具备 build + lint；后端具备 test + coverage。
- 新增/修改 API 不得引入业务 `any`；关键 DTO 有测试数据样例。
- 核心页面加载失败时均可见错误态或保留旧数据，不再无提示地吞掉异常。
- 聊天、活动报名、信息互换等关键状态流转有自动化测试。
- 未登录进入管理端受保护页面会立即跳转登录页；所有后台 API 仍由服务端权限校验。

