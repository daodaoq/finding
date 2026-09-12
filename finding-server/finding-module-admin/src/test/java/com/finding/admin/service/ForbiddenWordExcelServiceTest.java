package com.finding.admin.service;

import com.finding.common.BusinessException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.finding.admin.service.impl.ForbiddenWordExcelServiceImpl;

/** 违禁词 Excel/CSV 解析与模板生成单测。 */
class ForbiddenWordExcelServiceTest {

    private final ForbiddenWordExcelService service = new ForbiddenWordExcelServiceImpl();

    private byte[] xlsx(String... columnValues) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("违禁词");
            for (int i = 0; i < columnValues.length; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(columnValues[i]);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void parse_skipsHeaderRowAndBlanks() throws IOException {
        byte[] file = xlsx("违禁词", "词一", "", "   ", "词二");

        List<ForbiddenWordExcelService.ParsedWord> words =
                service.parse(new ByteArrayInputStream(file), "words.xlsx");

        assertEquals(2, words.size());
        assertEquals("词一", words.get(0).word());
        assertEquals("词二", words.get(1).word());
        // 表头在第 1 行,词条行号应为 2 与 5
        assertEquals(2, words.get(0).row());
        assertEquals(5, words.get(1).row());
    }

    @Test
    void parse_withoutHeader_keepsFirstRow() throws IOException {
        byte[] file = xlsx("第一行就是词", "第二行");

        List<ForbiddenWordExcelService.ParsedWord> words =
                service.parse(new ByteArrayInputStream(file), "words.xlsx");

        assertEquals(2, words.size());
        assertEquals("第一行就是词", words.get(0).word());
    }

    @Test
    void parse_csvTakesFirstColumn() {
        String csv = "违禁词,备注\n词甲,说明\n词乙\n";
        List<ForbiddenWordExcelService.ParsedWord> words =
                service.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "words.csv");

        assertEquals(2, words.size());
        assertEquals("词甲", words.get(0).word());
        assertEquals("词乙", words.get(1).word());
    }

    @Test
    void parse_unsupportedExtension_rejected() {
        assertThrows(BusinessException.class,
                () -> service.parse(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), "words.txt"));
    }

    @Test
    void parse_corruptFile_rejected() {
        assertThrows(BusinessException.class,
                () -> service.parse(new ByteArrayInputStream("not an excel".getBytes(StandardCharsets.UTF_8)), "words.xlsx"));
    }

    @Test
    void template_containsHeaderAndIsReadable() throws IOException {
        byte[] template = service.buildTemplate();

        assertTrue(template.length > 0);
        // 生成的模板应能被服务自身解析回来:首行是表头,应被跳过
        List<ForbiddenWordExcelService.ParsedWord> words =
                service.parse(new ByteArrayInputStream(template), "template.xlsx");
        assertEquals(2, words.size(), "模板含两行示例词");
        assertTrue(words.get(0).word().contains("示例"));
    }
}
