package com.finding.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finding.post.entity.Post;
import com.finding.post.mapper.PostMapper;
import com.finding.user.service.UserPostStatsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户动态数查询端口实现 —— 由 post 模块提供,注入给 user 模块使用。
 */
@Component
@RequiredArgsConstructor
public class UserPostStatsAdapter implements UserPostStatsQuery {

    private final PostMapper postMapper;

    @Override
    public int countPosts(Long userId) {
        return postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 1)).intValue();
    }
}
