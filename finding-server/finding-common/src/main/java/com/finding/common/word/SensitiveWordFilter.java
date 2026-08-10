package com.finding.common.word;

import com.finding.common.BusinessException;
import com.finding.common.RedisUtils;
import com.finding.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 违禁词过滤器 —— 所有用户内容发布入口在落库前调用 {@link #assertClean}。
 * <p>
 * 内存持有 Aho-Corasick 自动机,热路径 O(n) 纯内存扫描,无 Redis/DB 往返;
 * 词表来源:Redis 缓存 {@code forbidden:words}(TTL 1h) → 未命中回退 {@link WordProvider}(查 DB)。
 * <p>
 * 管理端增删改后调用 {@link #reloadFromSource()} 强制失效缓存并重建自动机,消除刷新窗口。
 */
@Component
@RequiredArgsConstructor
public class SensitiveWordFilter {

    private static final String CACHE_KEY = "forbidden:rules";
    private static final long CACHE_TTL_HOURS = 1;

    private final RedisUtils redisUtils;
    private final ObjectProvider<WordProvider> wordProvider;

    private final AtomicReference<AhoCorasick> automaton = new AtomicReference<>();
    /** 词 -> 动作(0=拦截 1=送审) */
    private final AtomicReference<Map<String, Integer>> actionMap = new AtomicReference<>();

    /**
     * 校验一组文本,任一命中违禁词即抛 {@link BusinessException},内容不落库。
     */
    public void assertClean(String... texts) {
        if (texts == null) {
            return;
        }
        assertClean(Arrays.asList(texts));
    }

    /**
     * 校验文本集合(可空),任一命中违禁词即抛 {@link BusinessException},内容不落库。
     */
    public void assertClean(Collection<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return;
        }
        ensureLoaded();
        AhoCorasick ac = automaton.get();
        Set<String> hits = new LinkedHashSet<>();
        for (String t : texts) {
            if (t == null || t.isEmpty()) {
                continue;
            }
            hits.addAll(ac.findAll(t));
        }
        if (!hits.isEmpty()) {
            // 提示具体命中了哪些违禁词,如:内容包含违禁词:「甲」「乙」
            String joined = hits.stream().map(w -> "「" + w + "」").collect(Collectors.joining());
            throw new BusinessException(ResultCode.CONTENT_BLOCKED, "内容包含违禁词:" + joined);
        }
    }

    /**
     * 强制从数据源重新加载(管理端增删改后调用):失效 Redis 缓存 → 重新读取 → 重建自动机。
     */
    public synchronized void reloadFromSource() {
        redisUtils.delete(CACHE_KEY);
        refresh();
    }

    private void ensureLoaded() {
        if (automaton.get() == null) {
            synchronized (this) {
                if (automaton.get() == null) {
                    refresh();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void refresh() {
        List<SensitiveWordRule> rules = redisUtils.get(CACHE_KEY);
        if (rules == null) {
            WordProvider provider = wordProvider.getIfAvailable();
            rules = provider != null ? provider.enabledRules() : Collections.emptyList();
            redisUtils.set(CACHE_KEY, rules, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
        Map<String, Integer> am = new HashMap<>();
        List<String> words = new ArrayList<>(rules.size());
        for (SensitiveWordRule r : rules) {
            words.add(r.word());
            am.put(r.word(), r.action());
        }
        actionMap.set(am);
        automaton.set(new AhoCorasick(words));
    }

    /**
     * 内容审核分类:按动作把命中的违禁词拆为「拦截」与「送审」。
     * 用于带审核流程的内容(动态)发布:拦截词必须拒绝,送审词进入审核队列。
     */
    public ReviewResult classifyReview(String... texts) {
        return classifyReview(Arrays.asList(texts));
    }

    public ReviewResult classifyReview(Collection<String> texts) {
        Set<String> blocking = new LinkedHashSet<>();
        Set<String> review = new LinkedHashSet<>();
        if (texts == null || texts.isEmpty()) {
            return new ReviewResult(blocking, review);
        }
        ensureLoaded();
        AhoCorasick ac = automaton.get();
        for (String t : texts) {
            if (t == null || t.isEmpty()) continue;
            for (String w : ac.findAll(t)) {
                if (actionOf(w) == 1) review.add(w);
                else blocking.add(w);
            }
        }
        return new ReviewResult(blocking, review);
    }

    private int actionOf(String word) {
        Map<String, Integer> am = actionMap.get();
        Integer a = am != null ? am.get(word) : null;
        return a != null ? a : 0;
    }
}
