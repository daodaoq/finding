package com.finding.common.word;

import java.util.Set;

/**
 * 内容审核分类结果 —— 按违禁词动作拆分。
 * <p>{@code blocking}:命中「拦截」动作的词(必须拒绝发布);
 * {@code review}:命中「送审」动作的词(进入审核队列)。</p>
 */
public record ReviewResult(Set<String> blocking, Set<String> review) {

    public boolean hasBlocking() {
        return blocking != null && !blocking.isEmpty();
    }

    public boolean hasReview() {
        return review != null && !review.isEmpty();
    }
}
