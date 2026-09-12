package com.finding.admin.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.finding.common.BusinessException;
import com.finding.common.ResultCode;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import com.finding.admin.service.ForbiddenWordExcelService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 违禁词 Excel/CSV 解析与模板生成(仅管理端使用)。
 *
 * <p>接受的表格形态:第一个工作表的第一列,每一行一个违禁词;首行若是表头(违禁词/word/...)
 * 会自动跳过;空行忽略。也接受 .csv(同样取第一列)。</p>
 */
@Service
public class ForbiddenWordExcelServiceImpl implements ForbiddenWordExcelService {

    /** 视为表头的首行取值(小写比较) */
    private static final Set<String> HEADER_CANDIDATES =
            Set.of("违禁词", "敏感词", "词", "word", "words", "keyword", "keywords");

    /** 单行文本长度上限,与 forbidden_word.word VARCHAR(100) 对齐 */
    private static final int MAX_WORD_LENGTH = 100;

    /**
     * 解析上传文件,返回第一列的非空词条(保留行号,便于前端展示失败位置)。
     * 首行表头会被跳过;不做去重(去重与查库比对由 ForbiddenWordService.importWords 统一处理)。
     */
    @Override
    public List<ParsedWord> parse(InputStream in, String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) {
                return parseCsv(in);
            }
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseWorkbook(in);
            }
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持 .xlsx / .xls / .csv 文件");
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件解析失败,请确认是有效的 Excel/CSV 文件");
        }
    }

    /** 生成导入模板:.xlsx,首行「违禁词」表头 + 两行示例,首列加宽 */
    @Override
    public byte[] buildTemplate() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("违禁词");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            Cell headerCell = header.createCell(0);
            headerCell.setCellValue("违禁词");
            headerCell.setCellStyle(headerStyle);

            Row sample1 = sheet.createRow(1);
            sample1.createCell(0).setCellValue("示例词(替换为真实违禁词)");
            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("另一个示例词");

            sheet.setColumnWidth(0, 40 * 256);   // 首列加宽,便于直接照填

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模板生成失败");
        }
    }

    // ── 内部实现 ──

    private List<ParsedWord> parseWorkbook(InputStream in) throws IOException {
        List<ParsedWord> words = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            if (wb.getNumberOfSheets() == 0) return words;
            Sheet sheet = wb.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                int rowNum = row.getRowNum() + 1;           // 展示用行号从 1 开始
                String value = cellText(row.getCell(0));
                if (firstRow) {
                    firstRow = false;
                    if (isHeader(value)) continue;          // 首行是表头则跳过
                }
                if (!value.isEmpty()) words.add(new ParsedWord(rowNum, value));
            }
        }
        return words;
    }

    private List<ParsedWord> parseCsv(InputStream in) throws IOException {
        List<ParsedWord> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String value = firstCsvColumn(line);
                if (firstLine) {
                    firstLine = false;
                    if (isHeader(value)) continue;
                }
                if (!value.isEmpty()) words.add(new ParsedWord(lineNo, value));
            }
        }
        return words;
    }

    /** 取 CSV 第一个字段:处理双引号包裹(内部 "" 转义),其余按逗号截断 */
    private String firstCsvColumn(String line) {
        String s = line.replace("﻿", "").trim();       // 去掉可能的 BOM
        if (s.startsWith("\"")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString().trim();
        }
        int comma = s.indexOf(',');
        return (comma >= 0 ? s.substring(0, comma) : s).trim();
    }

    /** 单元格文本:数字按整数/小数去尾输出,避免 "123.0" */
    private String cellText(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.STRING) return cell.getStringCellValue().trim();
        if (type == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
        }
        if (type == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        if (type == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (IllegalStateException e) {
                double d = cell.getNumericCellValue();
                return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
        }
        return "";
    }

    private boolean isHeader(String value) {
        return HEADER_CANDIDATES.contains(value.toLowerCase(Locale.ROOT));
    }

}
