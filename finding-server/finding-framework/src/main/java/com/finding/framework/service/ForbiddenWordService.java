package com.finding.framework.service;

import com.finding.common.PageVO;
import com.finding.framework.entity.ForbiddenWord;

import java.util.List;

/**
 * 违禁词管理 —— 增删改查 + 启用禁用,每次写操作后刷新内容过滤器。
 */
public interface ForbiddenWordService {

    /** 分页查询,keyword 对违禁词做模糊匹配,status 精确筛选 */
    PageVO<ForbiddenWord> page(int page, int size, String keyword, Integer status);

    /** 新增(默认启用,action 0=拦截 1=送审) */
    void create(String word, Integer action);

    /** 修改违禁词 */
    void update(Long id, String word, Integer action);

    /** 删除 */
    void delete(Long id);

    /** 启用/禁用 */
    void toggleStatus(Long id, Integer status);

    /**
     * 批量导入(Excel/CSV 解析后的词条)。
     *
     * <p>语义:文件内按小写去重、与库中已有词(同样按小写,因 DB 排序规则大小写不敏感)比对后跳过重复,
     * 逐条插入,最后<b>只刷新一次</b>内容过滤器(避免 N 次重建 AC 自动机)。导入的词一律为启用状态。</p>
     *
     * @param words  待导入词条(允许含空白/重复,由本方法归一)
     * @param action 0=拦截 1=送审(整批统一)
     */
    ForbiddenWordImportResult importWords(List<String> words, Integer action);
}
