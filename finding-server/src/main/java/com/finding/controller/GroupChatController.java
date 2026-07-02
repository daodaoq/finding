package com.finding.controller;

import com.finding.common.Result;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.GroupChatService;
import com.finding.vo.GroupChatVO;
import com.finding.vo.GroupMessageVO;
import com.finding.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat/groups")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;

    /** 我的群聊列表 */
    @GetMapping
    public Result<List<GroupChatVO>> listMyGroups() {
        return Result.ok(groupChatService.listMyGroups(JwtInterceptor.getCurrentUserId()));
    }

    /** 创建群聊 */
    @PostMapping
    public Result<GroupChatVO> createGroup(@RequestBody Map<String, Object> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<Integer> memberIdInts = (List<Integer>) body.get("memberIds");
        List<Long> memberIds = memberIdInts.stream().map(Long::valueOf).toList();
        return Result.ok(groupChatService.createGroup(userId, name, memberIds));
    }

    /** 群详情 */
    @GetMapping("/{id}")
    public Result<GroupChatVO> getGroupDetail(@PathVariable Long id) {
        return Result.ok(groupChatService.getGroupDetail(id, JwtInterceptor.getCurrentUserId()));
    }

    /** 可邀请用户 */
    @GetMapping("/{id}/invitable")
    public Result<List<Map<String, Object>>> getInvitableUsers(@PathVariable Long id) {
        return Result.ok(groupChatService.getInvitableUsers(JwtInterceptor.getCurrentUserId(), id));
    }

    /** 添加成员 */
    @PostMapping("/{id}/members")
    public Result<Void> addMembers(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("userIds");
        groupChatService.addMembers(JwtInterceptor.getCurrentUserId(), id,
                ids.stream().map(Long::valueOf).toList());
        return Result.ok();
    }

    /** 移除成员（群主专用） */
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupChatService.removeMember(JwtInterceptor.getCurrentUserId(), id, userId);
        return Result.ok();
    }

    /** 退出/解散群聊 */
    @DeleteMapping("/{id}")
    public Result<Void> leaveOrDisband(@PathVariable Long id) {
        groupChatService.leaveOrDisband(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 发送群消息 */
    @PostMapping("/{id}/send")
    public Result<GroupMessageVO> sendMessage(@PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        return Result.ok(groupChatService.sendMessage(id,
                JwtInterceptor.getCurrentUserId(),
                body.get("content"),
                body.getOrDefault("messageType", "text")));
    }

    /** 群消息历史 */
    @GetMapping("/{id}/messages")
    public Result<PageVO<GroupMessageVO>> getMessageHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Result.ok(groupChatService.getMessageHistory(id, page, size));
    }
}
