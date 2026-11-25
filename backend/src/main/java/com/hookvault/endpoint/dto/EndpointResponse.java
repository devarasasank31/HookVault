package com.hookvault.endpoint.dto;

import com.hookvault.endpoint.WebhookEndpoint;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class EndpointResponse {
    private UUID id;
    private String name;
    private String url;
    private String description;
    private boolean active;
    private Map<String, String> headers;
    private Instant createdAt;
    private Instant updatedAt;

    // Signing secret is only included on creation/rotation
    private String signingSecret;

    public static EndpointResponse from(WebhookEndpoint endpoint) {
        return from(endpoint, null);
    }

    public static EndpointResponse from(WebhookEndpoint endpoint, String signingSecret) {
        return EndpointResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .url(endpoint.getUrl())
                .description(endpoint.getDescription())
                .active(endpoint.isActive())
                .headers(endpoint.getHeaders())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .signingSecret(signingSecret)
                .build();
    }
}