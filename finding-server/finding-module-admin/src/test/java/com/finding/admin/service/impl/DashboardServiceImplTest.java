package com.finding.admin.service.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 看板质量指标纯函数:百分比与最久待处理时长。
 */
class DashboardServiceImplTest {

    @Test
    void percent_basic() {
        assertEquals(60.0, DashboardServiceImpl.percent(60, 100));
        assertEquals(25.0, DashboardServiceImpl.percent(50, 200));
        assertEquals(0.0, DashboardServiceImpl.percent(0, 100));
    }

    @Test
    void percent_roundsToTwoDecimals() {
        assertEquals(33.33, DashboardServiceImpl.percent(1, 3));
        assertEquals(66.67, DashboardServiceImpl.percent(2, 3));
    }

    @Test
    void percent_zeroDenominator_returnsZero() {
        assertEquals(0.0, DashboardServiceImpl.percent(10, 0));
        assertEquals(0.0, DashboardServiceImpl.percent(0, 0));
    }

    @Test
    void hoursAgo_computesElapsedHours() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 12, 0, 0);
        assertEquals(5, DashboardServiceImpl.hoursAgo(now.minusHours(5), now));
        assertEquals(0, DashboardServiceImpl.hoursAgo(now.minusMinutes(30), now));
    }

    @Test
    void hoursAgo_nullOrFuture_returnsZero() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 13, 12, 0, 0);
        assertEquals(0, DashboardServiceImpl.hoursAgo(null, now));
        assertEquals(0, DashboardServiceImpl.hoursAgo(now.plusHours(1), now));
    }
}
