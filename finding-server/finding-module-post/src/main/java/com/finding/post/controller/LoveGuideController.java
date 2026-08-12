package com.finding.post.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.common.util.XssUtil;
import com.finding.post.dto.LoveGuideCreateDTO;
import com.finding.post.entity.LoveGuide;
import com.finding.post.mapper.LoveGuideMapper;
import com.finding.user.security.JwtInterceptor;
import com.finding.user.common.VerificationGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/love-guides")
@RequiredArgsConstructor
public class LoveGuideController {
    private final LoveGuideMapper mapper;
    private final VerificationGuard verificationGuard;

    @GetMapping
    public Result<PageVO<LoveGuide>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "30") int size) {
        Page<LoveGuide> result = mapper.selectPage(new Page<>(page, Math.min(size, 50)), new LambdaQueryWrapper<LoveGuide>()
                .eq(LoveGuide::getReviewStatus, 1).orderByDesc(LoveGuide::getCreatedAt));
        return Result.ok(PageVO.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PostMapping
    public Result<LoveGuide> create(@Valid @RequestBody LoveGuideCreateDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) return Result.error(ResultCode.UNAUTHORIZED);
        verificationGuard.checkVerified(userId);
        LoveGuide guide = new LoveGuide();
        guide.setUserId(userId); guide.setTitle(XssUtil.clean(dto.getTitle())); guide.setSubtitle(XssUtil.clean(dto.getSubtitle()));
        guide.setContent(XssUtil.clean(dto.getContent())); guide.setCategory(XssUtil.clean(dto.getCategory())); guide.setReviewStatus(0);
        mapper.insert(guide);
        return Result.ok(guide);
    }
}
