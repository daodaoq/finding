package com.finding.controller;

import com.finding.common.Result;
import com.finding.common.VerificationGuard;
import com.finding.dto.MessageSendDTO;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.ChatService;
import com.finding.vo.ChatMessageVO;
import com.finding.vo.ConversationVO;
import com.finding.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天 REST 接口 —— 会话列表、消息历史、发送消息。
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final VerificationGuard verificationGuard;

    /** 获取当前用户的会话列表 */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> listConversations() {
        return Result.ok(chatService.listConversations(JwtInterceptor.getCurrentUserId()));
    }

    /** 创建或获取与指定用户的会话 */
    @PostMapping("/conversations")
    public Result<ConversationVO> getOrCreateConversation(@RequestParam Long targetUserId) {
        return Result.ok(chatService.getOrCreateConversation(
                JwtInterceptor.getCurrentUserId(), targetUserId));
    }

    /** 发送消息（REST 方式，也支持 WebSocket 发送） */
    @PostMapping("/send")
    public Result<ConversationVO> sendMessage(@Valid @RequestBody MessageSendDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        verificationGuard.checkVerified(userId); // 未认证用户不可发私信
        return Result.ok(chatService.sendMessage(userId, dto));
    }

    /** 获取会话消息历史（id=room_id） */
    @GetMapping("/conversations/{id}/messages")
    public Result<PageVO<ChatMessageVO>> getMessageHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "50") int size) {
        return Result.ok(chatService.getMessageHistory(
                JwtInterceptor.getCurrentUserId(), id, lastId, size));
    }

    /** 标记会话已读（id=room_id） */
    @PutMapping("/conversations/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        chatService.markConversationRead(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }
}
