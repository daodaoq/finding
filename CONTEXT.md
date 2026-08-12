# Finding 领域术语表(CONTEXT)

本文件固定校园论坛与相亲场景的核心术语与关系语义,供各模块实现、测试、工单与文档统一使用。以下概念**不可互相混用**。

## 关系与隐私术语

| 术语 | 定义 |
| --- | --- |
| 公开资料 | 任何人可见的基础信息:头像、昵称、学校、学生认证状态。 |
| 详细资料 | 受资料可见性控制的信息:性别、城市、个性签名等。未满足可见性条件时不得下发。 |
| 搜索可见性 | `user_settings.searchable`。关闭后用户不出现在「用户搜索」与「相亲推荐」中;不删除已有会话、关注与历史内容。 |
| 资料可见性 | `user_settings.profile_visible`。`1=所有人可见`、`2=仅信息互换后可见`(详见「信息互换」)。本人永远可查看完整资料。 |
| 联系权限 | `user_settings.friend_add_mode`。`0=自动同意`、`1=需对方同意`(默认)、`2=拒绝新的聊天申请`。只控制**建立新联系**,不影响既有会话。 |
| 拉黑 | 单向记录(`user_block`),双向任一方拉黑即生效:双方不能相互搜索、进入推荐流、关注、发起聊天申请、发送私信或查看对方详细资料。拉黑**不删除历史消息**。 |
| 信息互换 | 以双方都已同意的信息互换记录为准(`info_share` APPROVED);不等同于互相关注、聊天存在或搭子报名。 |
| 已认证用户 | 学生/实名认证状态为「已通过」的用户(`real_name_verified=2`)。 |
| 会话资格 | 允许向某人发起聊天申请或建立私聊会话的资格:双方未拉黑,且对方联系权限允许。 |

## 状态语义

- 聊天申请:`0=待处理 / 1=已通过 / 2=已拒绝`。被拒绝后可经冷却期再次申请,不永久阻止关系恢复。
- 搭子活动:`0=已取消 / 1=进行中 / 2=已关闭`;报名:`0=待处理 / 1=已通过 / 2=已拒绝`。
- 内容:`0=已删除 / 1=发布中 / 2=已下架`。

## 一致性要求

- 所有关系、隐私、状态与容量规则**必须由服务端最终裁决**,前端只做展示投影,不得只靠前端隐藏。
- 业务模块不得各自重复编写 `user_block` / `user_settings` 查询,统一走 `UserRelationshipService`(用户模块)。
- 错误码:拉黑用 `RELATION_BLOCKED(9101)`,联系权限拒绝用 `CONTACT_PERMISSION_DENIED(9102)`。

## 数据库迁移约定

**为什么必须幂等**:`deploy.sh` 每次部署都会把 `deploy/migrations/*.sql` **全部重跑一遍**(无 Flyway 类迁移追踪),所以每个迁移文件都必须能在已应用过的数据库上**重复执行成功**。

- 建表:一律 `CREATE TABLE IF NOT EXISTS`。
- 加列:MySQL 8 无 `ADD COLUMN IF NOT EXISTS`,用 `information_schema` 判断 + 存储过程动态执行(参考 `20260811_outbox_backoff_deadletter.sql` 的写法)。
- `MODIFY COLUMN`、`CREATE INDEX` 重复执行仅重置相同定义,属幂等,可直接用。
- 破坏性操作(删表/删列/清数据)**禁止**写入迁移;确需时用条件判断包住。
- 插入种子数据:用 `INSERT ... ON DUPLICATE KEY UPDATE` 或先查后插,不可裸 `INSERT`。
- **新增迁移必须在本地重复执行两遍验证**:
  ```bash
  docker exec -i finding-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" finding < deploy/migrations/xxx.sql
  ```
  第二次仍 rc=0 才算通过。
