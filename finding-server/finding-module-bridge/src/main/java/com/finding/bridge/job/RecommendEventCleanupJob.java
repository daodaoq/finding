package com.finding.bridge.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐行为事件(曝光/跳过/申请/通过)清理任务 —— 定期删除超过保留期的历史事件,防止 recommend_event 无限增长。
 * 与 ChatOutboxPublisher 一致使用 JdbcTemplate 直查,避免 MyBatis SQL 日志刷屏。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendEventCleanupJob {

    /** 事件保留天数 */
    private static final int RETENTION_DAYS = 90;
    /** 单批删除行数,分批避免长事务锁表 */
    private static final int BATCH = 5000;

    private final JdbcTemplate jdbcTemplate;

    /** 每天凌晨 3:17 执行(避开整点) */
    @Scheduled(cron = "0 17 3 * * *")
    public void cleanupOldEvents() {
        int total = 0;
        int rows;
        do {
            rows = jdbcTemplate.update(
                    "DELETE FROM recommend_event WHERE created_at < DATE_SUB(NOW(), INTERVAL " + RETENTION_DAYS
                            + " DAY) ORDER BY id LIMIT " + BATCH);
            total += rows;
        } while (rows == BATCH);
        if (total > 0) {
            log.info("Recommend event cleanup: removed {} rows older than {} days", total, RETENTION_DAYS);
        }
    }
}
