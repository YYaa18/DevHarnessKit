package com.acme.modern.account.repository;

import com.acme.modern.account.domain.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(String accountId);

    List<Account> findAll();

    Account save(Account account);
}
