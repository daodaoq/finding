package com.finding.admin.controller;

import com.finding.admin.service.AdminExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 管理员 - 数据导出(CSV)。
 * 供运营离线分析/归档,返回带 UTF-8 BOM 的 CSV(Excel 直接打开中文不乱码)。
 * 序列化逻辑见 {@link AdminExportService},这里只写响应头与响应体。
 */
@RestController
@RequestMapping("/api/v1/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final AdminExportService adminExportService;

    @GetMapping("/users")
    public void exportUsers(HttpServletResponse response) throws IOException {
        writeCsv(response, adminExportService.exportUsers());
    }

    @GetMapping("/posts")
    public void exportPosts(HttpServletResponse response) throws IOException {
        writeCsv(response, adminExportService.exportPosts());
    }

    private void writeCsv(HttpServletResponse response, AdminExportService.CsvFile file) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.filename());
        response.getOutputStream().write(file.bytes());
    }
}
