package com.hookvault.deadletter.dto;

import com.hookvault.deadletter.DeadLetterEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DeadLetterResponse {
    private UUID id;
    private UUID eventId;
    private UUID endpointId;
    private String eventType;
    private String failureReason;
    private int attemptCount;
    private Instant lastAttemptAt;
    private boolean resolved;
    private Instant resolvedAt;
    private UUID resolvedBy;
    private Instant createdAt;

    public static DeadLetterResponse from(DeadLetterEvent deadLetter) {
        return DeadLetterResponse.builder()
                .id(deadLetter.getId())
                .eventId(deadLetter.getEventId())
                .endpointId(deadLetter.getEndpointId())
                .eventType(deadLetter.getEvent() != null ? deadLetter.getEvent().getEventType() : null)
                .failureReason(deadLetter.getFailureReason())
                .attemptCount(deadLetter.getAttemptCount())
                .lastAttemptAt(deadLetter.getLastAttemptAt())
                .resolved(deadLetter.isResolved())
                .resolvedAt(deadLetter.getResolvedAt())
                .resolvedBy(deadLetter.getResolvedById())
                .createdAt(deadLetter.getCreatedAt())
                .build();
    }
}