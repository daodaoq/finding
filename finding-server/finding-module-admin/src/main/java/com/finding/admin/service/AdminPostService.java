package com.finding.admin.service;

import com.finding.common.PageVO;
import com.finding.post.dto.PostCreateDTO;

import java.util.Map;

/**
 * 管理员 - 动态管理。
 *
 * <p>审核语义:通过=发布(review_status 0),拒绝=记原因并通知作者;
 * <b>pass 缺省时按「不通过」处理</b>(拒绝为默认,避免请求体缺字段就放行内容)。</p>
 */
public interface AdminPostService {

    /** 动态列表(内容模糊/按 id 查询) */
    PageVO<Map<String, Object>> listPosts(int page, int size, String keyword);

    /** 编辑动态(管理端无所有权限制) */
    void updatePost(Long id, PostCreateDTO dto);

    /** 变更状态(0=已删除 1=发布中 2=已下架);非发布态通知作者 */
    void updatePostStatus(Long adminId, Long id, Map<String, Object> body);

    /** 设置置顶/精华(isTop/isHot 传 1 或 0;不传保持不变) */
    void updatePostFlag(Long adminId, Long id, Map<String, Object> body);

    /** 软删除(保留记录与审计)并通知作者 */
    void deletePost(Long adminId, Long id);

    /** 待审核队列(review_status=1 且未删除) */
    PageVO<Map<String, Object>> reviewQueue(int page, int size);

    /** 单条审核 */
    void reviewPost(Long adminId, Long id, Map<String, Object> body);

    /** 批量审核:先整体校验 ids,再在同一事务内逐条应用 */
    void reviewBatch(Long adminId, Map<String, Object> body);
}
