package com.finding.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.finding")
@MapperScan({
        "com.finding.user.mapper",
        "com.finding.message.mapper",
        "com.finding.post.mapper",
        "com.finding.mate.mapper",
        "com.finding.chat.mapper",
        "com.finding.bridge.mapper",
        "com.finding.group.mapper",
        "com.finding.framework.mapper",
        "com.finding.common.audit",
        "com.finding.app.mapper"
})
@EnableCaching
@EnableAsync
@EnableScheduling
public class FindingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FindingApplication.class, args);
    }
}
