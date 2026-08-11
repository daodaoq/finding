package com.finding.chat.controller;

import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.user.common.VerificationGuard;
import com.finding.chat.dto.ConversationSettingsDTO;
import com.finding.chat.dto.MessageSendDTO;
import com.finding.chat.dto.ReportDTO;
import com.finding.user.security.JwtInterceptor;
import com.finding.chat.service.ChatService;
import com.finding.chat.vo.ChatMessageVO;
import com.finding.chat.vo.ConversationSettingsVO;
import com.finding.message.vo.ConversationVO;
import com.finding.common.PageVO;
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
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(chatService.listConversations(userId));
    }

    /** 获取与指定用户的已有会话(不存在则拒绝——须先经聊天申请批准建立) */
    @PostMapping("/conversations")
    public Result<ConversationVO> getConversation(@RequestParam Long targetUserId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(chatService.getConversation(userId, targetUserId));
    }

    /** 发送消息(REST 方式) */
    @PostMapping("/send")
    public Result<ConversationVO> sendMessage(@Valid @RequestBody MessageSendDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        verificationGuard.checkVerified(userId); // 未认证用户不可发私信
        return Result.ok(chatService.sendMessage(userId, dto));
    }

    /** 获取会话消息历史（id=room_id） */
    @GetMapping("/conversations/{id}/messages")
    public Result<PageVO<ChatMessageVO>> getMessageHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(chatService.getMessageHistory(userId, id, lastId, size));
    }

    /** 标记会话已读（id=room_id） */
    @PutMapping("/conversations/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        chatService.markConversationRead(userId, id);
        return Result.ok();
    }

    /** 获取会话设置(置顶/免打扰/聊天背景) */
    @GetMapping("/conversations/{id}/settings")
    public Result<ConversationSettingsVO> getSettings(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(chatService.getConversationSettings(userId, id));
    }

    /** 更新会话设置(置顶/免打扰/聊天背景) */
    @PutMapping("/conversations/{id}/settings")
    public Result<Void> updateSettings(@PathVariable Long id, @RequestBody ConversationSettingsDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        chatService.updateConversationSettings(userId, id,
                dto.getPinned(), dto.getMuted(), dto.getBackground());
        return Result.ok();
    }

    /** 搜索会话内的聊天记录 */
    @GetMapping("/conversations/{id}/messages/search")
    public Result<PageVO<ChatMessageVO>> searchMessages(@PathVariable Long id,
                                                         @RequestParam String keyword,
                                                         @RequestParam(defaultValue = "50") int size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(chatService.searchMessages(userId, id, keyword, size));
    }

    /** 清空会话聊天记录 */
    @DeleteMapping("/conversations/{id}/messages")
    public Result<Void> clearMessages(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        chatService.clearMessages(userId, id);
        return Result.ok();
    }

    /** 投诉用户 */
    @PostMapping("/report")
    public Result<Void> report(@Valid @RequestBody ReportDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        chatService.reportUser(userId, dto.getTargetUserId(), dto.getRoomId(), dto.getReason());
        return Result.ok();
    }

    /** 撤回自己发送的消息(2分钟内) */
    @PostMapping("/messages/{messageId}/recall")
    public Result<Void> recallMessage(@PathVariable Long messageId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        chatService.recallMessage(userId, messageId);
        return Result.ok();
    }
}
