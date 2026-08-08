package com.finding.chat.service;

import com.finding.chat.vo.InfoShareStatusVO;

public interface InfoShareService {

    /** 发起互换申请(返回申请记录 id) */
    Long requestShare(Long fromUserId, Long toUserId);

    /** 处理互换申请(1=同意, 2=拒绝) */
    void handleShare(Long userId, Long shareId, Integer status);

    /** 查询我与对方的信息互换状态(聊天框按钮用) */
    InfoShareStatusVO getShareStatus(Long userId, Long otherUserId);
}
