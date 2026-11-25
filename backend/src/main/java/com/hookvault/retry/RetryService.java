package com.hookvault.retry;

import com.hookvault.delivery.DeliveryService;
import com.hookvault.event.WebhookEvent;
import com.hookvault.event.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final WebhookEventRepository eventRepository;
    private final DeliveryService deliveryService;

    // Run every 30 seconds to pick up stuck events
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processPendingRetries() {
        List<String> retryStatuses = List.of("PENDING", "RETRY_SCHEDULED", "FAILED");
        Instant cutoff = Instant.now().minusSeconds(60); // Events older than 1 minute

        List<WebhookEvent> events = eventRepository.findByStatusInAndCreatedAtAfter(retryStatuses, cutoff);

        for (WebhookEvent event : events) {
            try {
                // Only process if event is in a retryable state
                if (event.isPending() || "RETRY_SCHEDULED".equals(event.getStatus()) || event.isFailed()) {
                    log.debug("Processing retry for event {}", event.getId());
                    deliveryService.deliverEvent(event.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process retry for event {}: {}", event.getId(), e.getMessage());
            }
        }
    }

    // Clean up expired idempotency records daily
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    @Transactional
    public void cleanupExpiredIdempotencyRecords() {
        // This would be called via IdempotencyRecordRepository
        log.debug("Cleanup of expired idempotency records would run here");
    }
}