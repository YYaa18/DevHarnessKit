package com.acme.modern.account.service;

import com.acme.modern.account.dto.AccountResponse;
import com.acme.modern.account.repository.InMemoryAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountServiceTest {
    @Test
    void returnsAccountResponse() {
        AccountService service = new AccountService(new InMemoryAccountRepository(), new NoopAuditPublisher());

        AccountResponse response = service.getAccount("A-1001");

        assertEquals("Alice", response.getOwnerName());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void listsAccounts() {
        AccountService service = new AccountService(new InMemoryAccountRepository(), new NoopAuditPublisher());

        List<AccountResponse> responses = service.listAccounts();

        assertEquals(2, responses.size());
    }
}
