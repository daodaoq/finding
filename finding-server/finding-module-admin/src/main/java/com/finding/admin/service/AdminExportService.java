package com.finding.admin.service;

/**
 * 管理员 - 数据导出(CSV)。
 *
 * <p>供运营离线分析/归档:整表导出(不分页),返回带 UTF-8 BOM 的 CSV 字节
 * (Excel 直接打开中文不乱码)。序列化细节(表头文案、状态/性别文案、转义规则)
 * 全部收敛在这里,控制器只负责把文件名写进响应头、把字节写进响应体。</p>
 */
public interface AdminExportService {

    /**
     * 导出结果:下载文件名 + 完整 CSV 字节(已含 BOM、表头与数据行)。
     *
     * @param filename 建议的下载文件名(如 users.csv)
     * @param bytes    完整 CSV 内容
     */
    record CsvFile(String filename, byte[] bytes) {
    }

    /** 导出全部用户(id/昵称/手机号/学校/性别/状态/注册时间) */
    CsvFile exportUsers();

    /** 导出全部动态(id/用户 id/分类/内容/状态/审核状态/发布时间) */
    CsvFile exportPosts();
}
