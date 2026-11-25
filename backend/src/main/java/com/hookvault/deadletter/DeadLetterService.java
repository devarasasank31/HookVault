package com.hookvault.deadletter;

import com.hookvault.deadletter.dto.DeadLetterResponse;
import com.hookvault.event.WebhookEvent;
import com.hookvault.event.WebhookEventRepository;
import com.hookvault.exception.ForbiddenException;
import com.hookvault.exception.ResourceNotFoundException;
import com.hookvault.security.UserPrincipal;
import com.hookvault.delivery.DeliveryScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private final DeadLetterEventRepository deadLetterRepository;
    private final WebhookEventRepository eventRepository;
    private final DeliveryScheduler deliveryScheduler;

    @Transactional(readOnly = true)
    public DeadLetterResponse getDeadLetter(UUID userId, UUID deadLetterId) {
        DeadLetterEvent deadLetter = deadLetterRepository.findByIdAndUserId(deadLetterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dead Letter", deadLetterId));
        return DeadLetterResponse.from(deadLetter);
    }

    @Transactional
    public DeadLetterResponse replayDeadLetter(UUID userId, UUID deadLetterId) {
        DeadLetterEvent deadLetter = deadLetterRepository.findByIdAndUserId(deadLetterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dead Letter", deadLetterId));

        WebhookEvent originalEvent = deadLetter.getEvent();
        if (originalEvent == null) {
            throw new IllegalStateException("Original event not found for dead letter");
        }

        // Create a new event for replay (preserves original history)
        WebhookEvent replayEvent = WebhookEvent.builder()
                .endpoint(originalEvent.getEndpoint())
                .user(originalEvent.getUser())
                .eventType(originalEvent.getEventType())
                .payload(originalEvent.getPayload())
                .idempotencyKey(null) // No idempotency key for replay
                .status("PENDING")
                .build();

        replayEvent = eventRepository.save(replayEvent);

        // Schedule delivery for the replay
        deliveryScheduler.scheduleDelivery(replayEvent.getId());

        log.info("Replayed dead letter {} as new event {} by user {}", deadLetterId, replayEvent.getId(), userId);

        return DeadLetterResponse.from(deadLetter);
    }

    @Transactional
    public DeadLetterResponse resolveDeadLetter(UUID userId, UUID deadLetterId) {
        DeadLetterEvent deadLetter = deadLetterRepository.findByIdAndUserId(deadLetterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dead Letter", deadLetterId));

        if (deadLetter.isResolved()) {
            throw new IllegalStateException("Dead letter already resolved");
        }

        deadLetter.setResolved(true);
        deadLetter.setResolvedAt(Instant.now());
        deadLetter.setResolvedBy(null); // Would be set from principal in controller

        deadLetterRepository.save(deadLetter);
        log.info("Resolved dead letter {} by user {}", deadLetterId, userId);

        return DeadLetterResponse.from(deadLetter);
    }

    @Transactional
    public void dismissDeadLetter(UUID userId, UUID deadLetterId) {
        DeadLetterEvent deadLetter = deadLetterRepository.findByIdAndUserId(deadLetterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dead Letter", deadLetterId));

        deadLetterRepository.delete(deadLetter);
        log.info("Dismissed dead letter {} by user {}", deadLetterId, userId);
    }
}