package com.finding.common.word;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Aho-Corasick 多模式串匹配自动机。
 * <p>
 * 用于违禁词精确匹配(英文忽略大小写):构建一次后,单条文本扫描耗时与文本长度成正比,
 * 与词表大小无关,适合私聊/群聊等高频内容的热路径校验。
 * <p>
 * 线程安全:构建完成后为只读结构,可被多线程并发调用。
 */
public class AhoCorasick {

    private static final int ROOT = 0;

    /** 每个节点的子节点转移表(字符 → 节点号) */
    private final List<Map<Character, Integer>> next = new ArrayList<>();
    /** 每个节点的失配指针 */
    private final List<Integer> fail = new ArrayList<>();
    /** 每个节点可命中的模式串(自身或 fail 链继承而来),null 表示无 */
    private final List<String> output = new ArrayList<>();

    /**
     * @param patterns 模式串集合,空串/空白会忽略
     */
    public AhoCorasick(Collection<String> patterns) {
        next.add(new HashMap<>());
        fail.add(ROOT);
        output.add(null);
        if (patterns != null) {
            for (String p : patterns) {
                if (p == null) continue;
                String word = p.trim().toLowerCase();
                if (word.isEmpty()) continue;
                insert(word);
            }
        }
        buildFail();
    }

    private void insert(String word) {
        int cur = ROOT;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Map<Character, Integer> map = next.get(cur);
            int nxt = map.getOrDefault(c, -1);
            if (nxt == -1) {
                nxt = next.size();
                map.put(c, nxt);
                next.add(new HashMap<>());
                fail.add(ROOT);
                output.add(null);
            }
            cur = nxt;
        }
        output.set(cur, word);
    }

    private void buildFail() {
        Deque<Integer> queue = new ArrayDeque<>();
        // 根节点的直接子节点 fail 指向根
        for (Integer child : next.get(ROOT).values()) {
            fail.set(child, ROOT);
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            for (Map.Entry<Character, Integer> e : next.get(cur).entrySet()) {
                char c = e.getKey();
                int child = e.getValue();
                int f = fail.get(cur);
                while (f != ROOT && !next.get(f).containsKey(c)) {
                    f = fail.get(f);
                }
                Integer viaFail = next.get(f).get(c);
                fail.set(child, viaFail != null ? viaFail : ROOT);
                // 自身是终端则保留自身命中的模式串,否则继承 fail 节点的命中
                if (output.get(child) == null) {
                    output.set(child, output.get(fail.get(child)));
                }
                queue.add(child);
            }
        }
    }

    /**
     * 返回文本中命中的第一个模式串(词表顺序/字典序不确定,仅保证命中),未命中返回 null。
     */
    public String findFirst(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String s = text.toLowerCase();
        int cur = ROOT;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            while (cur != ROOT && !next.get(cur).containsKey(c)) {
                cur = fail.get(cur);
            }
            Integer nxt = next.get(cur).get(c);
            cur = nxt != null ? nxt : ROOT;
            if (output.get(cur) != null) {
                return output.get(cur);
            }
        }
        return null;
    }

    /** 文本是否包含任一模式串。 */
    public boolean contains(String text) {
        return findFirst(text) != null;
    }

    /**
     * 返回文本中所有命中的模式串(按首次出现顺序去重),未命中返回空列表。
     * 供「内容包含违禁词」的提示展示具体命中了哪些词。
     */
    public List<String> findAll(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        String s = text.toLowerCase();
        int cur = ROOT;
        LinkedHashSet<String> found = new LinkedHashSet<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            while (cur != ROOT && !next.get(cur).containsKey(c)) {
                cur = fail.get(cur);
            }
            Integer nxt = next.get(cur).get(c);
            cur = nxt != null ? nxt : ROOT;
            if (output.get(cur) != null) {
                found.add(output.get(cur));
            }
        }
        return new ArrayList<>(found);
    }
}
