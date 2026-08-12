# gen-hash —— BCrypt 密码哈希生成工具

## 用途
为种子数据 / 本地造测试账号时生成 BCrypt 密码哈希,直接写入 `user.password` 字段。
默认明文 `12345678`,可传参覆盖:`GenHash "my-password"`。

## 为什么放在 tools/ 而非 src/test
一次性、需要人工运行的开发工具,不属于可自动执行的测试。测试目录只保留
可由 CI / `mvn test` 自动执行的用例。

## 运行方式
该工具依赖 `spring-security-crypto`(项目已有)。用 Maven 将依赖写入 classpath 后运行:

```bash
cd finding-server
mvn -q -pl finding-app -am compile dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "finding-app/target/classes;$(cat finding-app/cp.txt)" com.finding.tools.GenHash
```

Windows PowerShell 下路径分隔用 `;`,Linux/macOS 用 `:`。
