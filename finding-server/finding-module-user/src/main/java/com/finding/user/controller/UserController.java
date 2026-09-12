package com.finding.user.controller;

import com.finding.common.Result;
import com.finding.common.PageQueryDTO;
import com.finding.user.dto.UserRemarkDTO;
import com.finding.user.security.JwtInterceptor;
import com.finding.user.service.UserService;
import com.finding.user.service.UserResumeService;
import com.finding.user.service.UserBlockService;
import com.finding.user.service.UserRemarkService;
import com.finding.user.service.ProfileCompletenessService;
import com.finding.common.PageVO;
import com.finding.user.vo.UserVO;
import com.finding.user.vo.ResumeViewVO;
import com.finding.user.vo.ProfileCompletenessVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserResumeService userResumeService;
    private final UserBlockService userBlockService;
    private final UserRemarkService userRemarkService;
    private final ProfileCompletenessService profileCompletenessService;

    /** 我的资料完整度(含缺失项,用于「还缺 xxx」引导) */
    @GetMapping("/me/completeness")
    public Result<ProfileCompletenessVO> myCompleteness() {
        return Result.ok(profileCompletenessService.completeness(JwtInterceptor.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        Long currentUserId = JwtInterceptor.getCurrentUserId();
        return Result.ok(userService.getUserProfile(id, currentUserId));
    }

    /** 查看他人情感简历(需已互换信息,否则返回锁定状态) */
    @GetMapping("/{id}/resume")
    public Result<ResumeViewVO> getResume(@PathVariable Long id) {
        Long currentUserId = JwtInterceptor.getCurrentUserId();
        return Result.ok(userResumeService.getResumeForView(currentUserId, id));
    }

    @PostMapping("/{id}/follow")
    public Result<Void> follow(@PathVariable Long id) {
        userService.followUser(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        userService.unfollowUser(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    @GetMapping("/{id}/followers")
    public Result<PageVO<UserVO>> followers(@PathVariable Long id, @Valid PageQueryDTO query) {
        return Result.ok(userService.getFollowers(id, query, JwtInterceptor.getCurrentUserId()));
    }

    @GetMapping("/{id}/following")
    public Result<PageVO<UserVO>> following(@PathVariable Long id, @Valid PageQueryDTO query) {
        return Result.ok(userService.getFollowing(id, query, JwtInterceptor.getCurrentUserId()));
    }

    @GetMapping("/{id}/mutual-follows")
    public Result<PageVO<UserVO>> mutualFollows(@PathVariable Long id, @Valid PageQueryDTO query) {
        return Result.ok(userService.getMutualFollows(id, query, JwtInterceptor.getCurrentUserId()));
    }

    @GetMapping("/search")
    public Result<PageVO<UserVO>> search(@RequestParam(required = false) String keyword,
                                          @Valid PageQueryDTO query) {
        return Result.ok(userService.searchUsers(keyword, query, JwtInterceptor.getCurrentUserId()));
    }

    /** 拉黑用户 */
    @PostMapping("/{id}/block")
    public Result<Void> block(@PathVariable Long id) {
        userBlockService.block(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 解除拉黑 */
    @DeleteMapping("/{id}/block")
    public Result<Void> unblock(@PathVariable Long id) {
        userBlockService.unblock(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 拉黑状态:blocked=我拉黑了对方, blockedBy=对方拉黑了我 */
    @GetMapping("/{id}/block-status")
    public Result<Map<String, Boolean>> blockStatus(@PathVariable Long id) {
        return Result.ok(userBlockService.blockStatus(JwtInterceptor.getCurrentUserId(), id));
    }

    // ── 备注(私密别名):设置后仅本人视角内以备注替换对方昵称 ──

    /** 设置/修改我对某人的备注 */
    @PostMapping("/{id}/remark")
    public Result<Void> setRemark(@PathVariable Long id, @RequestBody UserRemarkDTO dto) {
        userRemarkService.setRemark(JwtInterceptor.getCurrentUserId(), id, dto.getRemark());
        return Result.ok();
    }

    /** 清除我对某人的备注 */
    @DeleteMapping("/{id}/remark")
    public Result<Void> clearRemark(@PathVariable Long id) {
        userRemarkService.clearRemark(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 我对某人的备注;无备注返回空串(Result 有 NON_NULL,返回 null 会被省略) */
    @GetMapping("/{id}/remark")
    public Result<String> getRemark(@PathVariable Long id) {
        String remark = userRemarkService.getRemark(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok(remark == null ? "" : remark);
    }
}
