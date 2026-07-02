package com.finding.controller;

import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.VerificationGuard;
import com.finding.dto.ChatApplyDTO;
import com.finding.dto.ChatApplyHandleDTO;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.BridgeService;
import com.finding.vo.ChatApplyVO;
import com.finding.vo.HomeFeedVO;
import com.finding.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bridge")
@RequiredArgsConstructor
public class BridgeController {

    private final BridgeService bridgeService;
    private final VerificationGuard verificationGuard;

    /** 分页获取推荐用户列表 */
    @GetMapping("/recommend")
    public Result<PageVO<HomeFeedVO>> recommend(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Double lat,
                                                 @RequestParam(required = false) Double lng) {
        Long userId = JwtInterceptor.getCurrentUserId();
        return Result.ok(bridgeService.getRecommendFeed(userId, lat, lng, page, size));
    }

    /** 发送聊天申请 */
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody ChatApplyDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        verificationGuard.checkVerified(userId);
        bridgeService.applyChat(userId, dto.getToUserId(), dto.getRemark());
        return Result.ok();
    }

    /** 我发出的申请列表 */
    @GetMapping("/apply/sent")
    public Result<PageVO<ChatApplyVO>> sentApplies(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(bridgeService.getSentApplies(userId, page, size));
    }

    /** 我收到的申请列表 */
    @GetMapping("/apply/received")
    public Result<PageVO<ChatApplyVO>> receivedApplies(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(bridgeService.getReceivedApplies(userId, page, size));
    }

    /** 处理聊天申请（通过/拒绝） */
    @PutMapping("/apply/{id}/handle")
    public Result<Void> handleApply(@PathVariable Long id,
                                     @Valid @RequestBody ChatApplyHandleDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        bridgeService.handleApply(userId, id, dto.getStatus());
        return Result.ok();
    }
}
