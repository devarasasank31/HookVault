package com.hookvault.delivery;

public interface DeliveryScheduler {
    void scheduleDelivery(UUID eventId);
    void scheduleRetry(UUID eventId, int attemptNumber);
}