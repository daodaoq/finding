package com.finding.chat.controller;

import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.chat.dto.InfoShareHandleDTO;
import com.finding.chat.dto.InfoShareRequestDTO;
import com.finding.user.security.JwtInterceptor;
import com.finding.chat.service.InfoShareService;
import com.finding.chat.vo.InfoShareStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bridge/info-share")
@RequiredArgsConstructor
public class InfoShareController {

    private final InfoShareService infoShareService;

    /** 发起互换申请 */
    @PostMapping("/request")
    public Result<Void> request(@Valid @RequestBody InfoShareRequestDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        infoShareService.requestShare(userId, dto.getToUserId());
        return Result.ok();
    }

    /** 处理互换申请(1=同意, 2=拒绝) */
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody InfoShareHandleDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        infoShareService.handleShare(userId, id, dto.getStatus());
        return Result.ok();
    }

    /** 查询我与对方的信息互换状态(聊天框「互换信息」按钮) */
    @GetMapping("/status")
    public Result<InfoShareStatusVO> status(@RequestParam Long userId) {
        Long me = JwtInterceptor.getCurrentUserId();
        if (me == null) return Result.error(ResultCode.UNAUTHORIZED);
        return Result.ok(infoShareService.getShareStatus(me, userId));
    }
}
