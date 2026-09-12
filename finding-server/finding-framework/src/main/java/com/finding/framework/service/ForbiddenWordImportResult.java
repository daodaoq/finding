package com.finding.framework.service;

import java.util.List;

/**
 * 违禁词批量导入结果。
 *
 * @param total    解析出的非空词条数(未去重前的条数)
 * @param imported 成功入库数
 * @param skipped  因重复(文件内重复或库中已存在)而跳过的条数
 * @param failed   失败明细(如超长),不含重复项
 */
public record ForbiddenWordImportResult(int total, int imported, int skipped, List<FailedItem> failed) {

    /** 单条失败明细;row 由调用方(解析层)补全,服务层填 0 */
    public record FailedItem(int row, String word, String reason) {}
}
