package com.acme.modern.account.service;

import com.acme.modern.account.domain.Account;
import com.acme.modern.account.dto.AccountResponse;
import com.acme.modern.account.repository.AccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AccountService {
    private final AccountRepository accountRepository;
    private final AuditPublisher auditPublisher;

    public AccountService(AccountRepository accountRepository, AuditPublisher auditPublisher) {
        this.accountRepository = accountRepository;
        this.auditPublisher = auditPublisher;
    }

    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(notFound());
        return toResponse(account);
    }

    public List<AccountResponse> listAccounts() {
        List<AccountResponse> responses = new ArrayList<AccountResponse>();
        for (Account account : accountRepository.findAll()) {
            responses.add(toResponse(account));
        }
        return responses;
    }

    public AccountResponse suspendAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(notFound());
        Account saved = accountRepository.save(account.withStatus("SUSPENDED"));
        auditPublisher.publish("account.suspended", accountId);
        return toResponse(saved);
    }

    private Supplier<IllegalArgumentException> notFound() {
        return new Supplier<IllegalArgumentException>() {
            public IllegalArgumentException get() {
                return new IllegalArgumentException("account not found");
            }
        };
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getOwnerName(),
                account.getStatus(),
                account.getBalance());
    }
}
