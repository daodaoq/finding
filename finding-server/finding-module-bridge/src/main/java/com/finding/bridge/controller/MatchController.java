package com.finding.bridge.controller;

import com.finding.bridge.service.MatchService;
import com.finding.bridge.vo.MatchUserVO;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 双向心动配对 —— 喜欢/取消喜欢/我喜欢的人/喜欢我的人/互相喜欢。
 */
@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /** 心动 targetId;返回是否配对成功 */
    @PostMapping("/like/{targetId}")
    public Result<Map<String, Boolean>> like(@PathVariable Long targetId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        boolean matched = matchService.likeUser(userId, targetId);
        return Result.ok(Map.of("matched", matched));
    }

    /** 取消心动(已配对则同时解除配对) */
    @DeleteMapping("/like/{targetId}")
    public Result<Void> unlike(@PathVariable Long targetId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        matchService.unlikeUser(userId, targetId);
        return Result.ok();
    }

    /** 我喜欢的人 */
    @GetMapping("/likes/sent")
    public Result<PageVO<MatchUserVO>> myLikes(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(matchService.getMyLikes(userId, page, size));
    }

    /** 喜欢我的人 */
    @GetMapping("/likes/received")
    public Result<PageVO<MatchUserVO>> likesReceived(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(matchService.getLikesReceived(userId, page, size));
    }

    /** 互相喜欢(配对)列表 */
    @GetMapping("/matches")
    public Result<PageVO<MatchUserVO>> matches(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(matchService.getMyMatches(userId, page, size));
    }
}
