package com.acme.modern.account.controller;

import com.acme.modern.account.dto.AccountResponse;
import com.acme.modern.account.service.AccountService;
import com.acme.modern.account.support.HttpGet;
import com.acme.modern.account.support.HttpPost;

import java.util.List;

public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @HttpGet("/api/accounts/{accountId}")
    public AccountResponse getAccount(String accountId) {
        return accountService.getAccount(accountId);
    }

    @HttpGet("/api/accounts")
    public List<AccountResponse> listAccounts() {
        return accountService.listAccounts();
    }

    @HttpPost("/api/accounts/{accountId}/suspend")
    public AccountResponse suspendAccount(String accountId) {
        return accountService.suspendAccount(accountId);
    }
}
