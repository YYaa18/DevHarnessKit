package com.acme.legacy.order.util;

import java.util.List;

public class PageResult<T> {
    private final List<T> rows;
    private final int total;
    private final int pageNo;
    private final int pageSize;

    public PageResult(List<T> rows, int total, int pageNo, int pageSize) {
        this.rows = rows;
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public List<T> getRows() {
        return rows;
    }

    public int getTotal() {
        return total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }
}
