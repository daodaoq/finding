package com.finding.admin.service;

import com.finding.common.PageVO;

import java.util.Map;

/**
 * 管理员 - 评论管理。
 *
 * <p>列表补齐作者昵称与所属动态内容;编辑不限所有权;
 * 删除为软删(status=1,前端显示占位)并落审计,保留追溯记录。</p>
 */
public interface AdminCommentService {

    /** 评论列表(内容模糊 + 作者昵称 + 所属动态内容) */
    PageVO<Map<String, Object>> listComments(int page, int size, String keyword);

    /** 编辑评论内容(管理员可改一切) */
    void updateComment(Long id, Map<String, String> body);

    /** 软删除评论(保留审计,前端显示占位) */
    void deleteComment(Long adminId, Long id);
}
