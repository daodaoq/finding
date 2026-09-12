package com.finding.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finding.common.BusinessException;
import com.finding.common.PageVO;
import com.finding.common.ResultCode;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.common.word.SensitiveWordRule;
import com.finding.common.word.WordProvider;
import com.finding.framework.entity.ForbiddenWord;
import com.finding.framework.mapper.ForbiddenWordMapper;
import com.finding.framework.service.ForbiddenWordImportResult;
import com.finding.framework.service.ForbiddenWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 违禁词管理实现 —— 同时作为 {@link WordProvider},供内容过滤器查询启用词表。
 */
@Service
@RequiredArgsConstructor
public class ForbiddenWordServiceImpl implements ForbiddenWordService, WordProvider {

    /** 单条词长度上限(与 forbidden_word.word VARCHAR(100) 对齐) */
    private static final int MAX_WORD_LENGTH = 100;
    /** 单批导入上限:导入后需全量重建 AC 自动机,过大批次会拖慢且占内存 */
    private static final int MAX_IMPORT_SIZE = 2000;

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
    public void create(String word, Integer action) {
        requireWord(word);
        ForbiddenWord entity = new ForbiddenWord();
        entity.setWord(word.trim());
        entity.setStatus(1);
        entity.setAction(action != null && action == 1 ? 1 : 0);
        forbiddenWordMapper.insert(entity);
        sensitiveWordFilter.reloadFromSource();
    }

    @Override
    public void update(Long id, String word, Integer action) {
        requireWord(word);
        ForbiddenWord existing = requireExists(id);
        existing.setWord(word.trim());
        if (action != null) existing.setAction(action == 1 ? 1 : 0);
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
    @Transactional
    public ForbiddenWordImportResult importWords(List<String> words, Integer action) {
        if (words == null || words.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "没有可导入的违禁词");
        }
        if (words.size() > MAX_IMPORT_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "单次最多导入 " + MAX_IMPORT_SIZE + " 条,请拆分后分批上传");
        }

        List<ForbiddenWordImportResult.FailedItem> failed = new ArrayList<>();
        // 归一:去空白;超长(DB 列 VARCHAR(100))计入失败;文件内按小写去重
        LinkedHashMap<String, String> pending = new LinkedHashMap<>();  // lower -> 原词
        int skippedInFile = 0;
        for (String raw : words) {
            String word = raw == null ? "" : raw.trim();
            if (word.isEmpty()) continue;
            if (word.length() > MAX_WORD_LENGTH) {
                failed.add(new ForbiddenWordImportResult.FailedItem(0, word, "超过 " + MAX_WORD_LENGTH + " 字"));
                continue;
            }
            if (pending.putIfAbsent(word.toLowerCase(), word) != null) skippedInFile++;
        }
        if (pending.isEmpty() && failed.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "没有可导入的违禁词");
        }

        // 与库中已有词比对(DB 排序规则 utf8mb4_unicode_ci 大小写不敏感,故按小写归一)
        Set<String> existing = forbiddenWordMapper.selectList(
                        new LambdaQueryWrapper<ForbiddenWord>().select(ForbiddenWord::getWord))
                .stream()
                .map(f -> f.getWord() == null ? "" : f.getWord().toLowerCase())
                .collect(Collectors.toSet());

        int imported = 0;
        int skippedExisting = 0;
        int actionValue = action != null && action == 1 ? 1 : 0;
        for (Map.Entry<String, String> entry : pending.entrySet()) {
            if (existing.contains(entry.getKey())) {
                skippedExisting++;
                continue;
            }
            ForbiddenWord entity = new ForbiddenWord();
            entity.setWord(entry.getValue());
            entity.setStatus(1);
            entity.setAction(actionValue);
            forbiddenWordMapper.insert(entity);
            imported++;
        }

        // 批量导入只在末尾刷新一次(绝不能循环调 create(),否则每次都会重建 AC 自动机)
        if (imported > 0) {
            sensitiveWordFilter.reloadFromSource();
        }
        return new ForbiddenWordImportResult(
                words.size(), imported, skippedInFile + skippedExisting, failed);
    }

    @Override
    public List<SensitiveWordRule> enabledRules() {
        return forbiddenWordMapper.selectList(
                        new LambdaQueryWrapper<ForbiddenWord>().eq(ForbiddenWord::getStatus, 1))
                .stream()
                .map(f -> new SensitiveWordRule(f.getWord(), f.getAction() != null ? f.getAction() : 0))
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
