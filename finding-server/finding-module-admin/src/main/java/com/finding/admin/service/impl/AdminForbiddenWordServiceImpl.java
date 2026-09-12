package com.finding.admin.service.impl;

import com.finding.admin.service.AdminForbiddenWordService;
import com.finding.admin.service.ForbiddenWordExcelService;
import com.finding.admin.util.AdminPaging;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.audit.OperationAuditService;
import com.finding.framework.entity.ForbiddenWord;
import com.finding.framework.service.ForbiddenWordImportResult;
import com.finding.framework.service.ForbiddenWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminForbiddenWordServiceImpl implements AdminForbiddenWordService {

    private final ForbiddenWordService forbiddenWordService;
    private final ForbiddenWordExcelService excelService;
    private final OperationAuditService operationAuditService;

    @Override
    public PageVO<ForbiddenWord> list(int page, int size, String keyword, Integer status) {
        size = AdminPaging.clampSize(size);
        return forbiddenWordService.page(page, size, keyword, status);
    }

    @Override
    public void create(Map<String, Object> body) {
        Integer action = body.get("action") != null ? ((Number) body.get("action")).intValue() : 0;
        forbiddenWordService.create(String.valueOf(body.get("word")), action);
    }

    @Override
    public void update(Long id, Map<String, Object> body) {
        Integer action = body.get("action") != null ? ((Number) body.get("action")).intValue() : null;
        forbiddenWordService.update(id, String.valueOf(body.get("word")), action);
    }

    @Override
    public void toggleStatus(Long id, Map<String, Integer> body) {
        forbiddenWordService.toggleStatus(id, body.get("status"));
    }

    @Override
    public void delete(Long id) {
        forbiddenWordService.delete(id);
    }

    @Override
    public Map<String, Object> importWords(Long adminId, InputStream in, String filename, Integer action) {
        // 解析层已把格式/读取异常统一转成 PARAM_ERROR,这里只做业务编排
        List<ForbiddenWordExcelService.ParsedWord> parsed = excelService.parse(in, filename);
        if (parsed.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件里没有解析到违禁词,请确认第一列填了词");
        }

        ForbiddenWordImportResult result = forbiddenWordService.importWords(
                parsed.stream().map(ForbiddenWordExcelService.ParsedWord::word).toList(), action);

        operationAuditService.record(adminId, "forbidden_word_import", "forbidden_word", null,
                "Excel 批量导入违禁词",
                "成功 " + result.imported() + " 条 / 跳过 " + result.skipped() + " 条 / 失败 " + result.failed().size() + " 条");

        // 失败明细补上行号(服务层只认词,行号来自解析层)
        Map<String, Integer> rowByWord = new HashMap<>();
        for (ForbiddenWordExcelService.ParsedWord p : parsed) {
            rowByWord.putIfAbsent(p.word(), p.row());
        }
        List<Map<String, Object>> failed = new ArrayList<>();
        for (ForbiddenWordImportResult.FailedItem item : result.failed()) {
            Map<String, Object> row = new HashMap<>();
            row.put("row", rowByWord.getOrDefault(item.word(), 0));
            row.put("word", item.word());
            row.put("reason", item.reason());
            failed.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", result.total());
        data.put("imported", result.imported());
        data.put("skipped", result.skipped());
        data.put("failed", failed);
        return data;
    }

    @Override
    public byte[] buildTemplate() {
        return excelService.buildTemplate();
    }
}
