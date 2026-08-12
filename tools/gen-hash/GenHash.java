package com.finding.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 一次性辅助工具:为种子数据生成 BCrypt 密码哈希。
 *
 * 用途:本地造测试账号/给批量插入的种子用户设置密码时,生成可直接入库的 password 字段值。
 * 用法(依赖 spring-security-crypto,可在 finding-app 模块 classpath 下运行):
 *   mvn -q -pl finding-app -am compile dependency:build-classpath -Dmdep.outputFile=cp.txt
 *   java -cp "finding-app/target/classes;$(cat finding-app/cp.txt)" com.finding.tools.GenHash
 *
 * 注意:此工具仅供开发期使用,不属于自动化测试,勿放入 src/test。
 */
public class GenHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = args.length > 0 ? args[0] : "12345678";
        System.out.println("Hash for " + raw + ": " + encoder.encode(raw));
    }
}
