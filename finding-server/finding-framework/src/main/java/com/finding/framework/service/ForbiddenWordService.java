package com.finding.framework.service;

import com.finding.common.PageVO;
import com.finding.framework.entity.ForbiddenWord;

/**
 * 违禁词管理 —— 增删改查 + 启用禁用,每次写操作后刷新内容过滤器。
 */
public interface ForbiddenWordService {

    /** 分页查询,keyword 对违禁词做模糊匹配,status 精确筛选 */
    PageVO<ForbiddenWord> page(int page, int size, String keyword, Integer status);

    /** 新增(默认启用) */
    void create(String word);

    /** 修改违禁词 */
    void update(Long id, String word);

    /** 删除 */
    void delete(Long id);

    /** 启用/禁用 */
    void toggleStatus(Long id, Integer status);
}
