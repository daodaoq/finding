package com.finding.admin.controller;

import com.finding.admin.service.AdminForbiddenWordService;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.Result;
import com.finding.common.ResultCode;
import com.finding.framework.entity.ForbiddenWord;
import com.finding.user.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 管理员 - 违禁词管理(增删改查 + 启用禁用 + Excel 批量导入;业务逻辑见 {@link AdminForbiddenWordService})。
 * 每次写操作由框架内容过滤器即时生效。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminForbiddenWordController {

    private final AdminForbiddenWordService adminForbiddenWordService;

    /** 分页列表,支持按词模糊搜索与状态筛选 */
    @GetMapping("/forbidden-words")
    public Result<PageVO<ForbiddenWord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminForbiddenWordService.list(page, size, keyword, status));
    }

    /** 新增(默认启用;action 0=拦截 1=送审) */
    @PostMapping("/forbidden-words")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        adminForbiddenWordService.create(body);
        return Result.ok();
    }

    /** 修改违禁词 */
    @PutMapping("/forbidden-words/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminForbiddenWordService.update(id, body);
        return Result.ok();
    }

    /** 启用/禁用 */
    @PutMapping("/forbidden-words/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminForbiddenWordService.toggleStatus(id, body);
        return Result.ok();
    }

    /** 删除 */
    @DeleteMapping("/forbidden-words/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminForbiddenWordService.delete(id);
        return Result.ok();
    }

    /**
     * Excel/CSV 批量导入违禁词。
     * 表格只需一列(违禁词),首行表头自动跳过;导入即启用,action 由请求参数指定(0=拦截 1=送审)。
     */
    @PostMapping("/forbidden-words/import")
    public Result<Map<String, Object>> importWords(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "action", required = false) Integer action) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的 Excel/CSV 文件");
        }
        try (InputStream in = file.getInputStream()) {
            Map<String, Object> data = adminForbiddenWordService.importWords(
                    JwtInterceptor.getCurrentUserId(), in, file.getOriginalFilename(), action);
            return Result.ok(data);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件读取失败,请重试");
        }
    }

    /** 下载导入模板(.xlsx:首行「违禁词」表头 + 示例行) */
    @GetMapping("/forbidden-words/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] body = adminForbiddenWordService.buildTemplate();
        String filename = "forbidden-words-template.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
