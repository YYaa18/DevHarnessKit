package com.acme.modern.account.service;

public class NoopAuditPublisher implements AuditPublisher {
    public void publish(String eventType, String accountId) {
        // Testbed no-op publisher.
    }
}
