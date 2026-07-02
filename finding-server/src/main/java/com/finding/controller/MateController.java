package com.finding.controller;

import com.finding.common.Result;
import com.finding.common.VerificationGuard;
import com.finding.dto.MateCreateDTO;
import com.finding.dto.MateQueryDTO;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.MateService;
import com.finding.vo.MateVO;
import com.finding.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mates")
@RequiredArgsConstructor
public class MateController {

    private final MateService mateService;
    private final VerificationGuard verificationGuard;

    @GetMapping
    public Result<PageVO<MateVO>> list(@Valid MateQueryDTO query) {
        return Result.ok(mateService.listInvitations(query, JwtInterceptor.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<MateVO> detail(@PathVariable Long id) {
        return Result.ok(mateService.getInvitationDetail(id, JwtInterceptor.getCurrentUserId()));
    }

    @PostMapping
    public Result<MateVO> create(@Valid @RequestBody MateCreateDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        verificationGuard.checkVerified(userId); // 未认证用户不可创建搭子
        return Result.ok(mateService.createInvitation(userId, dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MateCreateDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        mateService.updateInvitation(userId, id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        mateService.cancelInvitation(userId, id);
        return Result.ok();
    }

    @PostMapping("/{id}/join")
    public Result<Void> join(@PathVariable Long id, @RequestParam(required = false) String message) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        verificationGuard.checkVerified(userId); // 未认证用户不可加入搭子
        mateService.joinInvitation(userId, id, message);
        return Result.ok();
    }

    @DeleteMapping("/{id}/leave")
    public Result<Void> leave(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        mateService.leaveInvitation(userId, id);
        return Result.ok();
    }

    @PutMapping("/{id}/participants/{participantId}")
    public Result<Void> handleJoin(@PathVariable Long id,
                                    @PathVariable Long participantId,
                                    @RequestParam boolean accept) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        mateService.handleJoinRequest(userId, id, participantId, accept);
        return Result.ok();
    }

    /** 我发布的搭子邀约 */
    @GetMapping("/my")
    public Result<PageVO<MateVO>> myInvitations(@Valid MateQueryDTO query) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        return Result.ok(mateService.getMyInvitations(userId, query));
    }

    /** 我加入的搭子邀约 */
    @GetMapping("/my-joined")
    public Result<PageVO<MateVO>> myJoined(@Valid MateQueryDTO query) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(com.finding.common.ResultCode.UNAUTHORIZED);
        return Result.ok(mateService.getMyJoinedInvitations(userId, query));
    }

    @GetMapping("/categories")
    public Result<List<Map<String, String>>> categories() {
        List<Map<String, String>> list = List.of(
                Map.of("code", "travel", "name", "旅游搭子", "icon", "✈️"),
                Map.of("code", "carpool", "name", "拼车搭子", "icon", "🚗"),
                Map.of("code", "fitness", "name", "健身搭子", "icon", "💪"),
                Map.of("code", "study", "name", "学习搭子", "icon", "📚"),
                Map.of("code", "exam", "name", "备考搭子", "icon", "📝"),
                Map.of("code", "sports", "name", "运动搭子", "icon", "⚽"),
                Map.of("code", "gaming", "name", "游戏搭子", "icon", "🎮"),
                Map.of("code", "entertainment", "name", "娱乐搭子", "icon", "🎬"),
                Map.of("code", "other", "name", "其他", "icon", "📌")
        );
        return Result.ok(list);
    }
}
