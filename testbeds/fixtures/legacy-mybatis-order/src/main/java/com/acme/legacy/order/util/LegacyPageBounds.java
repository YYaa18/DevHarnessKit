package com.acme.legacy.order.util;

public final class LegacyPageBounds {
    private LegacyPageBounds() {
    }

    public static int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return 1;
        }
        return pageNo;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 20;
        }
        return pageSize;
    }
}
