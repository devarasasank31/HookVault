package com.hookvault.event;

import com.hookvault.event.dto.EventResponse;
import com.hookvault.event.dto.IngestEventRequest;
import com.hookvault.exception.ConflictException;
import com.hookvault.exception.ForbiddenException;
import com.hookvault.exception.ResourceNotFoundException;
import com.hookvault.exception.ValidationException;
import com.hookvault.security.UserPrincipal;
import com.hookvault.endpoint.WebhookEndpoint;
import com.hookvault.endpoint.WebhookEndpointRepository;
import com.hookvault.user.UserRepository;
import com.hookvault.delivery.DeliveryScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final WebhookEventRepository eventRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final UserRepository userRepository;
    private final DeliveryScheduler deliveryScheduler;

    private static final int IDEMPOTENCY_TTL_DAYS = 30;

    @Transactional
    public EventResponse ingestEvent(UserPrincipal principal, IngestEventRequest request) {
        // Verify endpoint ownership
        WebhookEndpoint endpoint = endpointRepository.findByIdAndUserId(request.getEndpointId(), principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", request.getEndpointId()));

        if (!endpoint.isActive()) {
            throw new ValidationException("Endpoint is not active");
        }

        // Check idempotency
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            if (idempotencyRepository.existsByUserIdAndIdempotencyKey(principal.getId(), request.getIdempotencyKey())) {
                // Return existing event
                WebhookEvent existingEvent = eventRepository.findByIdempotencyKey(request.getIdempotencyKey())
                        .orElseThrow(() -> new IllegalStateException("Idempotency record exists but event not found"));
                log.info("Duplicate event ingestion attempt with idempotency key: {}", request.getIdempotencyKey());
                return EventResponse.from(existingEvent);
            }
        }

        // Create event
        WebhookEvent event = WebhookEvent.builder()
                .endpoint(endpoint)
                .user(endpoint.getUser())
                .eventType(request.getEventType())
                .payload(request.getPayload())
                .idempotencyKey(request.getIdempotencyKey())
                .status("PENDING")
                .build();

        event = eventRepository.save(event);

        // Create idempotency record if key provided
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            var idempotencyRecord = IdempotencyRecord.builder()
                    .user(endpoint.getUser())
                    .idempotencyKey(request.getIdempotencyKey())
                    .event(event)
                    .expiresAt(Instant.now().plusSeconds(IDEMPOTENCY_TTL_DAYS * 24 * 60 * 60))
                    .build();
            idempotencyRepository.save(idempotencyRecord);
        }

        // Schedule delivery
        deliveryScheduler.scheduleDelivery(event.getId());

        log.info("Ingested event {} for endpoint {} by user {}", event.getId(), endpoint.getId(), principal.getId());
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(UserPrincipal principal, UUID eventId) {
        WebhookEvent event = eventRepository.findByIdAndUserId(eventId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listEvents(
            UserPrincipal principal,
            String endpointId,
            String eventType,
            String status,
            Pageable pageable
    ) {
        UUID endpointUuid = endpointId != null ? UUID.fromString(endpointId) : null;

        Page<WebhookEvent> events;
        if (endpointUuid != null) {
            // Verify endpoint ownership
            if (!endpointRepository.existsByIdAndUserId(endpointUuid, principal.getId())) {
                throw new ForbiddenException("Endpoint not accessible");
            }
            events = eventRepository.findByEndpointIdWithFilters(endpointUuid, eventType, status, pageable);
        } else {
            events = eventRepository.findByUserIdWithFilters(principal.getId(), eventType, status, pageable);
        }

        return events.map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public long getEventCount(UserPrincipal principal, String status) {
        if (status != null) {
            return eventRepository.countByUserIdAndStatus(principal.getId(), status);
        }
        return eventRepository.countByUserId(principal.getId());
    }

    @Transactional
    public void updateEventStatus(UUID eventId, String status) {
        eventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(status);
            if ("DELIVERED".equals(status)) {
                event.setDeliveredAt(Instant.now());
            }
            eventRepository.save(event);
        });
    }
}