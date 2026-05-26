package com.acme.modern.account.repository;

import com.acme.modern.account.domain.Account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> accounts = new LinkedHashMap<String, Account>();

    public InMemoryAccountRepository() {
        save(new Account("A-1001", "Alice", "ACTIVE", new BigDecimal("1200.00")));
        save(new Account("A-1002", "Bob", "SUSPENDED", new BigDecimal("50.00")));
    }

    public Optional<Account> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    public List<Account> findAll() {
        return new ArrayList<Account>(accounts.values());
    }

    public Account save(Account account) {
        accounts.put(account.getAccountId(), account);
        return account;
    }
}
