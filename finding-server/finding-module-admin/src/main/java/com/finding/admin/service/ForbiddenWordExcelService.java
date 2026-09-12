package com.finding.admin.service;

import java.io.InputStream;
import java.util.List;

/**
 * 违禁词 Excel/CSV 解析与模板生成(仅管理端使用)。
 *
 * <p>接受的表格形态:第一个工作表的第一列,每一行一个违禁词;首行若是表头(违禁词/word/...)
 * 会自动跳过;空行忽略。也接受 .csv(同样取第一列)。</p>
 */
public interface ForbiddenWordExcelService {

    /** 解析结果:词条 + 原始行号(用于失败明细定位,行号从 1 开始) */
    record ParsedWord(int row, String word) {}

    /**
     * 解析上传文件,返回第一列的非空词条(保留行号,便于前端展示失败位置)。
     * 首行表头会被跳过;不做去重(去重与查库比对由 ForbiddenWordService.importWords 统一处理)。
     */
    List<ParsedWord> parse(InputStream in, String filename);

    /** 生成导入模板:.xlsx,首行「违禁词」表头 + 两行示例,首列加宽 */
    byte[] buildTemplate();
}
