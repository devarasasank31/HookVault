package com.hookvault.delivery;

import com.hookvault.delivery.dto.DeliveryResult;
import com.hookvault.delivery.DeliveryScheduler;
import com.hookvault.endpoint.WebhookEndpoint;
import com.hookvault.endpoint.WebhookEndpointRepository;
import com.hookvault.event.WebhookEvent;
import com.hookvault.event.WebhookEventRepository;
import com.hookvault.exception.ResourceNotFoundException;
import com.hookvault.deadletter.DeadLetterEvent;
import com.hookvault.deadletter.DeadLetterEventRepository;
import com.hookvault.retry.RetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class DeliveryService {

    private final WebhookEventRepository eventRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final DeadLetterEventRepository deadLetterRepository;
    private final HttpDeliveryClient httpDeliveryClient;
    private final HmacSigner hmacSigner;
    private final DeliveryScheduler deliveryScheduler;
    private final RetryService retryService;

    /**
     * DeliverySchedulerImpl depends on this service (it invokes deliverEvent
     * from scheduled tasks) while this service asks the scheduler to queue
     * retries. Injecting the scheduler lazily breaks that constructor cycle;
     * the scheduler is only touched at delivery time, never during
     * construction.
     */
    public DeliveryService(
            WebhookEventRepository eventRepository,
            WebhookEndpointRepository endpointRepository,
            DeliveryAttemptRepository attemptRepository,
            DeadLetterEventRepository deadLetterRepository,
            HttpDeliveryClient httpDeliveryClient,
            HmacSigner hmacSigner,
            @Lazy DeliveryScheduler deliveryScheduler,
            RetryService retryService
    ) {
        this.eventRepository = eventRepository;
        this.endpointRepository = endpointRepository;
        this.attemptRepository = attemptRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.httpDeliveryClient = httpDeliveryClient;
        this.hmacSigner = hmacSigner;
        this.deliveryScheduler = deliveryScheduler;
        this.retryService = retryService;
    }

    @Transactional
    public void deliverEvent(UUID eventId) {
        WebhookEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        // Skip if already delivered or dead-lettered
        if (event.isDelivered() || event.isDeadLettered()) {
            log.debug("Event {} already in terminal state: {}", eventId, event.getStatus());
            return;
        }

        WebhookEndpoint endpoint = endpointRepository.findById(event.getEndpointId())
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", event.getEndpointId()));

        if (!endpoint.isActive()) {
            log.warn("Endpoint {} is not active, marking event {} as failed", endpoint.getId(), eventId);
            handleDeliveryFailure(event, endpoint, "Endpoint is not active", 0, null);
            return;
        }

        int attemptNumber = (int) attemptRepository.countByEventId(eventId) + 1;

        // Create delivery attempt record
        DeliveryAttempt attempt = createAttemptRecord(event, endpoint, attemptNumber);

        try {
            // Deliver the webhook
            DeliveryResult result = httpDeliveryClient.deliver(endpoint, event, attemptNumber);

            // Update attempt with result
            completeAttempt(attempt, result);

            if (result.isSuccess()) {
                handleDeliverySuccess(event, attempt);
            } else {
                handleDeliveryFailure(event, endpoint, result.getErrorMessage(), attemptNumber, result.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Delivery failed for event {}: {}", eventId, e.getMessage());
            completeAttemptWithError(attempt, e.getMessage());
            handleDeliveryFailure(event, endpoint, e.getMessage(), attemptNumber, null);
        }
    }

    private DeliveryAttempt createAttemptRecord(WebhookEvent event, WebhookEndpoint endpoint, int attemptNumber) {
        String payload = toJsonString(event.getPayload());
        String signature = hmacSigner.sign(payload, endpoint.getSigningSecret());

        DeliveryAttempt attempt = DeliveryAttempt.builder()
                .event(event)
                .endpoint(endpoint)
                .attemptNumber(attemptNumber)
                .requestUrl(endpoint.getUrl())
                .requestMethod("POST")
                .requestHeaders(buildHeaders(endpoint, event, signature, attemptNumber))
                .requestBody(event.getPayload())
                .status("IN_PROGRESS")
                .build();

        return attemptRepository.save(attempt);
    }

    private java.util.Map<String, String> buildHeaders(WebhookEndpoint endpoint, WebhookEvent event, String signature, int attemptNumber) {
        java.util.Map<String, String> headers = new java.util.HashMap<>(endpoint.getHeaders());
        headers.put("Content-Type", "application/json");
        headers.put("X-HookVault-Event-ID", event.getId().toString());
        headers.put("X-HookVault-Event-Type", event.getEventType());
        headers.put("X-HookVault-Signature", signature);
        headers.put("X-HookVault-Delivery-Attempt", String.valueOf(attemptNumber));
        headers.put("User-Agent", "HookVault/1.0");
        return headers;
    }

    private void completeAttempt(DeliveryAttempt attempt, DeliveryResult result) {
        attempt.setResponseStatus(result.getStatusCode());
        attempt.setResponseBody(truncateResponseBody(result.getBody()));
        attempt.setResponseHeaders(result.getHeaders());
        attempt.setDurationMs(result.getDurationMs());
        attempt.setCompletedAt(java.time.Instant.now());
        attempt.setStatus(result.isSuccess() ? "SUCCESS" : "FAILURE");
        attemptRepository.save(attempt);
    }

    private void completeAttemptWithError(DeliveryAttempt attempt, String errorMessage) {
        attempt.setErrorMessage(errorMessage);
        attempt.setCompletedAt(java.time.Instant.now());
        attempt.setStatus("FAILURE");
        attemptRepository.save(attempt);
    }

    private void handleDeliverySuccess(WebhookEvent event, DeliveryAttempt attempt) {
        event.setStatus("DELIVERED");
        event.setDeliveredAt(java.time.Instant.now());
        eventRepository.save(event);
        log.info("Event {} delivered successfully on attempt {}", event.getId(), attempt.getAttemptNumber());
    }

    private void handleDeliveryFailure(WebhookEvent event, WebhookEndpoint endpoint, String errorMessage, int attemptNumber, Integer statusCode) {
        // Check if we should retry
        boolean shouldRetry = shouldRetry(attemptNumber, statusCode);
        int maxRetries = 5; // Default, could be configurable

        if (shouldRetry && attemptNumber < maxRetries) {
            event.setStatus("RETRY_SCHEDULED");
            eventRepository.save(event);
            deliveryScheduler.scheduleRetry(event.getId(), attemptNumber);
            log.info("Event {} scheduled for retry {} (attempt {}/{})", event.getId(), attemptNumber + 1, attemptNumber, maxRetries);
        } else {
            // Move to dead letter queue
            moveToDeadLetter(event, endpoint, errorMessage, attemptNumber);
        }
    }

    private boolean shouldRetry(int attemptNumber, Integer statusCode) {
        if (attemptNumber >= 5) {
            return false;
        }

        // Don't retry client errors (4xx) except 429 (rate limit) and 408 (timeout)
        if (statusCode != null && statusCode >= 400 && statusCode < 500) {
            return statusCode == 429 || statusCode == 408;
        }

        return true;
    }

    private void moveToDeadLetter(WebhookEvent event, WebhookEndpoint endpoint, String failureReason, int attemptCount) {
        event.setStatus("DEAD_LETTERED");
        eventRepository.save(event);

        DeadLetterEvent deadLetter = DeadLetterEvent.builder()
                .event(event)
                .endpoint(endpoint)
                .user(event.getUser())
                .failureReason(failureReason)
                .attemptCount(attemptCount)
                .lastAttemptAt(java.time.Instant.now())
                .resolved(false)
                .build();

        deadLetterRepository.save(deadLetter);
        log.warn("Event {} moved to dead letter queue after {} attempts: {}", event.getId(), attemptCount, failureReason);
    }

    private String toJsonString(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    private String truncateResponseBody(String body) {
        if (body == null) return null;
        int maxLength = 10000; // 10KB limit
        return body.length() > maxLength ? body.substring(0, maxLength) + "... [truncated]" : body;
    }
}