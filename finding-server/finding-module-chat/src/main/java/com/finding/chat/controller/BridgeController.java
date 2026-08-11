package com.finding.chat.controller;

import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.user.common.VerificationGuard;
import com.finding.chat.dto.ChatApplyDTO;
import com.finding.chat.dto.ChatApplyHandleDTO;
import com.finding.user.security.JwtInterceptor;
import com.finding.chat.dto.UserMatchPreferenceDTO;
import com.finding.chat.entity.UserMatchPreference;
import com.finding.chat.service.BridgeService;
import com.finding.chat.vo.ChatApplyVO;
import com.finding.chat.vo.HomeFeedVO;
import com.finding.common.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bridge")
@RequiredArgsConstructor
public class BridgeController {

    private final BridgeService bridgeService;
    private final VerificationGuard verificationGuard;

    /** 分页获取推荐用户列表(仅登录用户可见真实推荐数据) */
    @GetMapping("/recommend")
    public Result<PageVO<HomeFeedVO>> recommend(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Double lat,
                                                 @RequestParam(required = false) Double lng) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
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

    /** 我收到的待处理申请数（情书入口角标） */
    @GetMapping("/apply/received/pending-count")
    public Result<Map<String, Long>> receivedPendingCount() {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(Map.of("count", bridgeService.countPendingReceived(userId)));
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

    /** 撤回我发出的待处理申请 */
    @PostMapping("/apply/{id}/withdraw")
    public Result<Void> withdrawApply(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        bridgeService.withdrawApply(userId, id);
        return Result.ok();
    }

    /** 我的相亲交友偏好 */
    @GetMapping("/preference")
    public Result<UserMatchPreference> getPreference() {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(bridgeService.getMatchPreference(userId));
    }

    /** 更新相亲交友偏好 */
    @PutMapping("/preference")
    public Result<Void> updatePreference(@Valid @RequestBody UserMatchPreferenceDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        bridgeService.updateMatchPreference(userId, dto.toEntity());
        return Result.ok();
    }

    /** 对某候选「不感兴趣」:排除出推荐流 */
    @PostMapping("/recommend/{targetId}/skip")
    public Result<Void> skip(@PathVariable Long targetId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        bridgeService.skipUser(userId, targetId);
        return Result.ok();
    }
}
