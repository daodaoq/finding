package com.finding.common.util;

import java.util.regex.Pattern;

/**
 * 轻量 XSS 过滤 —— 去除用户文本中的危险 HTML/脚本片段。
 * <p>不依赖第三方库,基于正则清洗:script 标签、事件处理器属性、javascript:/vbscript: 协议、危险标签。
 * 用于用户内容入库前清洗(与 {@code SensitiveWordFilter} 的违禁词校验配合)。
 */
public final class XssUtil {

    private static final Pattern SCRIPT_TAG =
            Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_OPEN =
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_HANDLER =
            Pattern.compile("\\son\\w+\\s*=\\s*('[^']*'|\"[^\"]*\"|[^\\s>]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_URI = Pattern.compile("(?i)\\bjavascript:");
    private static final Pattern VB_URI = Pattern.compile("(?i)\\bvbscript:");
    private static final Pattern DANGEROUS_TAG =
            Pattern.compile("</?(?:iframe|object|embed|form|link|meta|base|applet|style)[^>]*>", Pattern.CASE_INSENSITIVE);

    private XssUtil() {
    }

    /** 清洗用户文本,去除危险片段;null/空串原样返回。 */
    public static String clean(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String s = text;
        s = SCRIPT_TAG.matcher(s).replaceAll("");
        s = SCRIPT_OPEN.matcher(s).replaceAll("");
        s = EVENT_HANDLER.matcher(s).replaceAll("");
        s = JAVASCRIPT_URI.matcher(s).replaceAll("");
        s = VB_URI.matcher(s).replaceAll("");
        s = DANGEROUS_TAG.matcher(s).replaceAll("");
        return s;
    }
}
