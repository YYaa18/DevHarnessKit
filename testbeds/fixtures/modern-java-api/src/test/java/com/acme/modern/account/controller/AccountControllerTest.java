package com.acme.modern.account.controller;

import com.acme.modern.account.dto.AccountResponse;
import com.acme.modern.account.repository.InMemoryAccountRepository;
import com.acme.modern.account.service.AccountService;
import com.acme.modern.account.service.NoopAuditPublisher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountControllerTest {
    @Test
    void delegatesGetAccountToService() {
        AccountService service = new AccountService(new InMemoryAccountRepository(), new NoopAuditPublisher());
        AccountController controller = new AccountController(service);

        AccountResponse response = controller.getAccount("A-1001");

        assertEquals("A-1001", response.getAccountId());
    }
}
