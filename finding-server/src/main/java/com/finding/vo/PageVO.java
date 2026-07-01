package com.finding.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {

    private List<T> records;
    private Long total;
    private Integer page;
    private Integer size;
    private Boolean hasMore;

    public static <T> PageVO<T> of(List<T> records, Long total, Integer page, Integer size) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setSize(size);
        vo.setHasMore((long) page * size < total);
        return vo;
    }
}
