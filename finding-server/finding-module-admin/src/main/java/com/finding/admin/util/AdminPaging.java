package com.finding.admin.util;

/**
 * 管理端分页参数防护。
 *
 * <p>接口仍接受 page/size 查询参数(不改前端契约),但在 service 内统一夹紧,
 * 避免 {@code ?size=1000000} 这类请求把整表拉进内存。</p>
 */
public final class AdminPaging {

    /** 单页上限 */
    public static final int MAX_SIZE = 100;
    /** 默认单页大小(与既有 @RequestParam(defaultValue="10") 保持一致) */
    public static final int DEFAULT_SIZE = 10;

    private AdminPaging() {
    }

    /** 夹紧 size 到 [1, MAX_SIZE];<=0 时回落到默认值 */
    public static int clampSize(int size) {
        if (size <= 0) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }

    /** 夹紧 page 到 >=1 */
    public static int clampPage(int page) {
        return Math.max(page, 1);
    }
}
