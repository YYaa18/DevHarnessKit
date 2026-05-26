package com.acme.modern.account.dto;

import java.math.BigDecimal;

public class AccountResponse {
    private final String accountId;
    private final String ownerName;
    private final String status;
    private final BigDecimal balance;

    public AccountResponse(String accountId, String ownerName, String status, BigDecimal balance) {
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
}
