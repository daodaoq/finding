package com.finding.admin.service;

import com.finding.common.PageVO;
import com.finding.framework.entity.ForbiddenWord;

import java.io.InputStream;
import java.util.Map;

/**
 * 管理员 - 违禁词管理(增删改查 + 启用禁用 + Excel 批量导入)。
 *
 * <p>增删改查委托给框架的 {@code ForbiddenWordService}(每次写操作刷新内容过滤器,全站发布入口即时生效);
 * 本服务负责管理端的编排:请求体取值、上传文件解析、导入审计与失败明细分页行号的补全。</p>
 */
public interface AdminForbiddenWordService {

    /** 分页列表,支持按词模糊搜索与状态筛选 */
    PageVO<ForbiddenWord> list(int page, int size, String keyword, Integer status);

    /** 新增(默认启用;body.action 0=拦截 1=送审) */
    void create(Map<String, Object> body);

    /** 修改违禁词 */
    void update(Long id, Map<String, Object> body);

    /** 启用/禁用 */
    void toggleStatus(Long id, Map<String, Integer> body);

    /** 删除 */
    void delete(Long id);

    /**
     * Excel/CSV 批量导入违禁词。
     *
     * <p>表格只需一列(违禁词),首行表头自动跳过;导入即启用,action 由请求参数指定(0=拦截 1=送审)。
     * 解析失败/无有效词条按 {@code PARAM_ERROR} 抛出;导入成功后写审计,并把解析层行号补回失败明细。</p>
     *
     * @param adminId  操作管理员 id(审计用)
     * @param in       上传文件流(由调用方负责关闭)
     * @param filename 原始文件名(用于判断 xlsx/csv)
     * @param action   0=拦截 1=送审(整批统一)
     * @return 导入结果:{@code total/imported/skipped/failed[{row,word,reason}]}
     */
    Map<String, Object> importWords(Long adminId, InputStream in, String filename, Integer action);

    /** 生成导入模板(.xlsx:首行「违禁词」表头 + 示例行) */
    byte[] buildTemplate();
}
