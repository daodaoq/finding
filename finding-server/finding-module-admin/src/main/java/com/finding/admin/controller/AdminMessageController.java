package com.finding.admin.controller;

import com.finding.admin.service.AdminMessageService;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 站内通知与聊天记录(业务逻辑见 {@link AdminMessageService})。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMessageController {

    private final AdminMessageService adminMessageService;

    /** 给单个用户发系统通知 */
    @PostMapping("/messages")
    public Result<Void> sendMessage(@RequestBody Map<String, Object> body) {
        adminMessageService.sendMessage(body);
        return Result.ok();
    }

    /** 给全部用户广播系统通知 */
    @PostMapping("/messages/broadcast")
    public Result<Void> broadcast(@RequestBody Map<String, Object> body) {
        adminMessageService.broadcast(body);
        return Result.ok();
    }

    /** 按用户查看私聊消息(内容审查),可选 otherUserId 只看两人对话 */
    @GetMapping("/messages/chat")
    public Result<PageVO<Map<String, Object>>> chatMessages(
            @RequestParam Long userId,
            @RequestParam(required = false) Long otherUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminMessageService.chatMessages(userId, otherUserId, page, size));
    }

    /** 删除单条私聊消息 */
    @DeleteMapping("/messages/chat/{id}")
    public Result<Void> deleteChatMessage(@PathVariable Long id) {
        adminMessageService.deleteChatMessage(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    /** 按群/按发送用户查看群聊消息(内容审查) */
    @GetMapping("/messages/group")
    public Result<PageVO<Map<String, Object>>> groupMessages(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminMessageService.groupMessages(groupId, userId, page, size));
    }

    /** 删除单条群聊消息 */
    @DeleteMapping("/messages/group/{id}")
    public Result<Void> deleteGroupMessage(@PathVariable Long id) {
        adminMessageService.deleteGroupMessage(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }
}
