package com.finding;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.finding.mapper")
@EnableCaching
public class FindingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FindingApplication.class, args);
    }
}
