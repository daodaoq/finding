package com.finding.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.common.word.WordProvider;
import com.finding.framework.entity.ForbiddenWord;
import com.finding.framework.mapper.ForbiddenWordMapper;
import com.finding.framework.service.ForbiddenWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 违禁词管理实现 —— 同时作为 {@link WordProvider},供内容过滤器查询启用词表。
 */
@Service
@RequiredArgsConstructor
public class ForbiddenWordServiceImpl implements ForbiddenWordService, WordProvider {

    private final ForbiddenWordMapper forbiddenWordMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    public PageVO<ForbiddenWord> page(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<ForbiddenWord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ForbiddenWord::getWord, keyword);
        }
        if (status != null) {
            wrapper.eq(ForbiddenWord::getStatus, status);
        }
        wrapper.orderByDesc(ForbiddenWord::getCreatedAt);
        Page<ForbiddenWord> result = forbiddenWordMapper.selectPage(new Page<>(page, size), wrapper);
        return PageVO.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public void create(String word) {
        requireWord(word);
        ForbiddenWord entity = new ForbiddenWord();
        entity.setWord(word.trim());
        entity.setStatus(1);
        forbiddenWordMapper.insert(entity);
        sensitiveWordFilter.reloadFromSource();
    }

    @Override
    public void update(Long id, String word) {
        requireWord(word);
        ForbiddenWord existing = requireExists(id);
        existing.setWord(word.trim());
        forbiddenWordMapper.updateById(existing);
        sensitiveWordFilter.reloadFromSource();
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        forbiddenWordMapper.deleteById(id);
        sensitiveWordFilter.reloadFromSource();
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        ForbiddenWord existing = requireExists(id);
        existing.setStatus(status != null && status == 1 ? 1 : 0);
        forbiddenWordMapper.updateById(existing);
        sensitiveWordFilter.reloadFromSource();
    }

    @Override
    public List<String> enabledWords() {
        return forbiddenWordMapper.selectList(
                        new LambdaQueryWrapper<ForbiddenWord>().eq(ForbiddenWord::getStatus, 1))
                .stream()
                .map(ForbiddenWord::getWord)
                .collect(Collectors.toList());
    }

    private void requireWord(String word) {
        if (!StringUtils.hasText(word)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "违禁词不能为空");
        }
    }

    private ForbiddenWord requireExists(Long id) {
        ForbiddenWord existing = forbiddenWordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "违禁词不存在");
        }
        return existing;
    }
}
