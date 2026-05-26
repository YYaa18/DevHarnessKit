package com.acme.legacy.shop.dto;

public class ShopOrderQuery {
    private String customerNo;
    private String status;
    private String legacyAction;
    private Integer pageNo;
    private Integer pageSize;

    public String getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLegacyAction() {
        return legacyAction;
    }

    public void setLegacyAction(String legacyAction) {
        this.legacyAction = legacyAction;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
