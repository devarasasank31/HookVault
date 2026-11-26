package com.hookvault.delivery;

import java.util.UUID;

public interface DeliveryScheduler {
    void scheduleDelivery(UUID eventId);
    void scheduleRetry(UUID eventId, int attemptNumber);
}