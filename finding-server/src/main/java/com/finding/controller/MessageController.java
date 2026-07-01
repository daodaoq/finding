package com.finding.controller;

import com.finding.common.Result;
import com.finding.dto.PageQueryDTO;
import com.finding.interceptor.JwtInterceptor;
import com.finding.service.MessageService;
import com.finding.vo.ConversationVO;
import com.finding.vo.MessageVO;
import com.finding.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<PageVO<MessageVO>> list(@RequestParam(required = false) String type,
                                           @Valid PageQueryDTO query) {
        return Result.ok(messageService.listMessages(JwtInterceptor.getCurrentUserId(), type, query));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", messageService.getUnreadCount(JwtInterceptor.getCurrentUserId())));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markAsRead(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestParam(required = false) String type) {
        messageService.markAllAsRead(JwtInterceptor.getCurrentUserId(), type);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.deleteMessage(JwtInterceptor.getCurrentUserId(), id);
        return Result.ok();
    }

    @GetMapping("/conversations")
    public Result<PageVO<ConversationVO>> conversations(@Valid PageQueryDTO query) {
        return Result.ok(messageService.listConversations(JwtInterceptor.getCurrentUserId(), query));
    }
}
