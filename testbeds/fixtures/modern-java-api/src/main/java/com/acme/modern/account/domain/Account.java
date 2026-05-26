package com.acme.modern.account.domain;

import java.math.BigDecimal;

public class Account {
    private final String accountId;
    private final String ownerName;
    private final String status;
    private final BigDecimal balance;

    public Account(String accountId, String ownerName, String status, BigDecimal balance) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.status = status;
        this.balance = balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Account withStatus(String newStatus) {
        return new Account(accountId, ownerName, newStatus, balance);
    }
}
