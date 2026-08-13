package com.finding.app.controller;

import com.finding.common.Result;
import com.finding.framework.websocket.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 在线状态查询 —— 基于 Redis 心跳键,前端用于展示实时在线。
 */
@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final OnlineStatusService onlineStatusService;

    /** 单个用户在线状态 */
    @GetMapping("/online/{userId}")
    public Result<Map<String, Boolean>> online(@PathVariable Long userId) {
        return Result.ok(Map.of("online", onlineStatusService.isOnline(userId)));
    }

    /** 批量在线状态:请求体为 userId 数组,返回 userId → online */
    @PostMapping("/online")
    public Result<Map<Long, Boolean>> onlineBatch(@RequestBody List<Long> userIds) {
        return Result.ok(onlineStatusService.isOnlineBatch(userIds));
    }
}
