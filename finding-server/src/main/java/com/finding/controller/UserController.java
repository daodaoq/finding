package com.finding.controller;

import com.finding.common.Result;
import com.finding.dto.PageQueryDTO;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.UserService;
import com.finding.vo.PageVO;
import com.finding.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        Long currentUserId = JwtInterceptor.getCurrentUserId();
        return Result.ok(userService.getUserProfile(id, currentUserId));
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
