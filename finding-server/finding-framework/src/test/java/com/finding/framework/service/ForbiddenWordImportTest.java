package com.finding.framework.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finding.common.BusinessException;
import com.finding.common.word.SensitiveWordFilter;
import com.finding.framework.entity.ForbiddenWord;
import com.finding.framework.mapper.ForbiddenWordMapper;
import com.finding.framework.service.impl.ForbiddenWordServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 违禁词批量导入单测 —— 去重/跳过/超长,以及"只刷新一次过滤器"的关键约束。 */
class ForbiddenWordImportTest {

    @Mock
    private ForbiddenWordMapper forbiddenWordMapper;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    private ForbiddenWordServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 纯单测无 MyBatis 上下文:手动注册实体,使 LambdaQueryWrapper 能解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ForbiddenWord.class);
        service = new ForbiddenWordServiceImpl(forbiddenWordMapper, sensitiveWordFilter);
    }

    private void noExistingWords() {
        when(forbiddenWordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(forbiddenWordMapper.insert(any())).thenReturn(1);
    }

    /** 关键回归:批量导入只在末尾刷新一次自动机,绝不能每个词刷一次 */
    @Test
    void importWords_refreshesFilterExactlyOnce() {
        noExistingWords();

        service.importWords(Arrays.asList("词A", "词B", "词C"), 0);

        verify(sensitiveWordFilter, times(1)).reloadFromSource();
        verify(forbiddenWordMapper, times(3)).insert(any());
    }

    @Test
    void importWords_dedupesWithinFileIgnoringCase() {
        noExistingWords();

        ForbiddenWordImportResult result = service.importWords(Arrays.asList("Bad", "bad", "BAD", "other"), 0);

        assertEquals(4, result.total());
        assertEquals(2, result.imported());   // Bad / other
        assertEquals(2, result.skipped());    // bad / BAD
    }

    @Test
    void importWords_skipsWordsAlreadyInDb() {
        ForbiddenWord existing = new ForbiddenWord();
        existing.setWord("已存在");
        when(forbiddenWordMapper.selectList(any())).thenReturn(List.of(existing));
        when(forbiddenWordMapper.insert(any())).thenReturn(1);

        ForbiddenWordImportResult result = service.importWords(Arrays.asList("已存在", "新词"), 0);

        assertEquals(1, result.imported());
        assertEquals(1, result.skipped());
    }

    /** 库中大小写不同的词也算重复(DB 排序规则大小写不敏感) */
    @Test
    void importWords_dbMatchIsCaseInsensitive() {
        ForbiddenWord existing = new ForbiddenWord();
        existing.setWord("WORD");
        when(forbiddenWordMapper.selectList(any())).thenReturn(List.of(existing));

        ForbiddenWordImportResult result = service.importWords(List.of("word"), 0);

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        verify(forbiddenWordMapper, never()).insert(any());
        verify(sensitiveWordFilter, never()).reloadFromSource();  // 无插入则不刷新
    }

    @Test
    void importWords_tooLongGoesToFailed() {
        noExistingWords();
        String tooLong = "长".repeat(101);

        ForbiddenWordImportResult result = service.importWords(Arrays.asList("正常词", tooLong), 0);

        assertEquals(1, result.imported());
        assertEquals(1, result.failed().size());
        assertEquals(tooLong, result.failed().get(0).word());
    }

    @Test
    void importWords_trimsAndIgnoresBlank() {
        noExistingWords();

        ForbiddenWordImportResult result = service.importWords(Arrays.asList("  空格词  ", "   ", ""), 0);

        ArgumentCaptor<ForbiddenWord> captor = ArgumentCaptor.forClass(ForbiddenWord.class);
        verify(forbiddenWordMapper).insert(captor.capture());
        assertEquals("空格词", captor.getValue().getWord());
        assertEquals(1, captor.getValue().getStatus(), "导入的词默认启用");
        assertEquals(0, captor.getValue().getAction());
    }

    @Test
    void importWords_actionOne_setsReviewAction() {
        noExistingWords();

        service.importWords(List.of("送审词"), 1);

        ArgumentCaptor<ForbiddenWord> captor = ArgumentCaptor.forClass(ForbiddenWord.class);
        verify(forbiddenWordMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getAction());
    }

    @Test
    void importWords_empty_throws() {
        assertThrows(BusinessException.class, () -> service.importWords(Collections.emptyList(), 0));
        assertThrows(BusinessException.class, () -> service.importWords(List.of("  ", ""), 0));
    }

    @Test
    void importWords_tooMany_throws() {
        List<String> many = new java.util.ArrayList<>();
        for (int i = 0; i < 2001; i++) many.add("w" + i);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.importWords(many, 0));
        assertTrue(ex.getMessage().contains("拆"));
        verify(forbiddenWordMapper, never()).insert(any());
    }
}
