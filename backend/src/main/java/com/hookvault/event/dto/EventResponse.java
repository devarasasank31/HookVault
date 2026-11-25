package com.hookvault.event.dto;

import com.hookvault.event.WebhookEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class EventResponse {
    private UUID id;
    private UUID endpointId;
    private String eventType;
    private Map<String, Object> payload;
    private String idempotencyKey;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deliveredAt;

    public static EventResponse from(WebhookEvent event) {
        return EventResponse.builder()
                .id(event.getId())
                .endpointId(event.getEndpointId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .idempotencyKey(event.getIdempotencyKey())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .deliveredAt(event.getDeliveredAt())
                .build();
    }
}