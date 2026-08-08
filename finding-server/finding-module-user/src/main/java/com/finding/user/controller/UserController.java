package com.finding.user.controller;

import com.finding.common.Result;
import com.finding.common.PageQueryDTO;
import com.finding.user.security.JwtInterceptor;
import com.finding.user.service.UserService;
import com.finding.user.service.UserResumeService;
import com.finding.common.PageVO;
import com.finding.user.vo.UserVO;
import com.finding.user.vo.ResumeViewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserResumeService userResumeService;

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
        return Result.ok(userService.getFollowers(id, query));
    }

    @GetMapping("/{id}/following")
    public Result<PageVO<UserVO>> following(@PathVariable Long id, @Valid PageQueryDTO query) {
        return Result.ok(userService.getFollowing(id, query));
    }

    @GetMapping("/search")
    public Result<PageVO<UserVO>> search(@RequestParam(required = false) String keyword,
                                          @Valid PageQueryDTO query) {
        return Result.ok(userService.searchUsers(keyword, query));
    }
}
