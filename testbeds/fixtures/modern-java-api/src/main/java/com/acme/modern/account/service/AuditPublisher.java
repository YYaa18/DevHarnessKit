package com.acme.modern.account.service;

public interface AuditPublisher {
    void publish(String eventType, String accountId);
}
