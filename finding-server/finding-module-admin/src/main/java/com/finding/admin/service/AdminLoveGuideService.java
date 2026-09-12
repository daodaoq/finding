package com.finding.admin.service;

import com.finding.common.PageVO;
import com.finding.post.entity.LoveGuide;

import java.util.Map;

/**
 * 管理员 - 恋爱经验投稿审核。
 *
 * <p>列表展示全部投稿(待审核/已通过/已拒绝),按提交时间倒序,便于管理端回看历史;
 * 审核动作把 reviewStatus 置为 1(通过)/2(拒绝)并记录审核人与时间,
 * 拒绝时通知作者(附原因)。</p>
 */
public interface AdminLoveGuideService {

    /** 投稿队列(全状态,按提交时间倒序) */
    PageVO<LoveGuide> queue(int page, int size);

    /** 审核投稿:body.pass=true 通过;false 拒绝(通知作者并写审计) */
    void review(Long adminId, Long id, Map<String, Object> body);
}
