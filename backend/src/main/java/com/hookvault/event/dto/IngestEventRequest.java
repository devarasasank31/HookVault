package com.hookvault.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class IngestEventRequest {

    @NotBlank(message = "Event type is required")
    @Size(max = 100, message = "Event type must not exceed 100 characters")
    private String eventType;

    @NotNull(message = "Endpoint ID is required")
    private UUID endpointId;

    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;

    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    private String idempotencyKey;
}