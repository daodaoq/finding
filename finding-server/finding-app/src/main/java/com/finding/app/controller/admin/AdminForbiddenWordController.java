package com.finding.app.controller.admin;

import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.framework.entity.ForbiddenWord;
import com.finding.framework.service.ForbiddenWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员 - 违禁词管理(增删改查 + 启用禁用)。
 * 每次写操作由 {@link ForbiddenWordService} 刷新内容过滤器,全站发布入口即时生效。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminForbiddenWordController {

    private final ForbiddenWordService forbiddenWordService;

    /** 分页列表,支持按词模糊搜索与状态筛选 */
    @GetMapping("/forbidden-words")
    public Result<PageVO<ForbiddenWord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(forbiddenWordService.page(page, size, keyword, status));
    }

    /** 新增(默认启用;action 0=拦截 1=送审) */
    @PostMapping("/forbidden-words")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Integer action = body.get("action") != null ? ((Number) body.get("action")).intValue() : 0;
        forbiddenWordService.create(String.valueOf(body.get("word")), action);
        return Result.ok();
    }

    /** 修改违禁词 */
    @PutMapping("/forbidden-words/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer action = body.get("action") != null ? ((Number) body.get("action")).intValue() : null;
        forbiddenWordService.update(id, String.valueOf(body.get("word")), action);
        return Result.ok();
    }

    /** 启用/禁用 */
    @PutMapping("/forbidden-words/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        forbiddenWordService.toggleStatus(id, body.get("status"));
        return Result.ok();
    }

    /** 删除 */
    @DeleteMapping("/forbidden-words/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        forbiddenWordService.delete(id);
        return Result.ok();
    }
}
