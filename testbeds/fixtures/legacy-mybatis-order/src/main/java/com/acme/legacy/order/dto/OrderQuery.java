package com.acme.legacy.order.dto;

public class OrderQuery {
    private String customerName;
    private String status;
    private Boolean paidOnly;
    private Integer pageNo;
    private Integer pageSize;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getPaidOnly() {
        return paidOnly;
    }

    public void setPaidOnly(Boolean paidOnly) {
        this.paidOnly = paidOnly;
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
