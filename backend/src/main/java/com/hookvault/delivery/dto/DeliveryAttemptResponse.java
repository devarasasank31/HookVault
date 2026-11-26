package com.hookvault.delivery.dto;

import com.hookvault.delivery.DeliveryAttempt;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DeliveryAttemptResponse {
    private UUID id;
    private UUID eventId;
    private UUID endpointId;
    private int attemptNumber;
    private String requestUrl;
    private String requestMethod;
    private Map<String, String> requestHeaders;
    private Map<String, Object> requestBody;
    private Integer responseStatus;
    private String responseBody;
    private Map<String, String> responseHeaders;
    private Long durationMs;
    private String errorMessage;
    private String status;
    private Instant attemptedAt;
    private Instant completedAt;

    public static DeliveryAttemptResponse from(DeliveryAttempt attempt) {
        return DeliveryAttemptResponse.builder()
                .id(attempt.getId())
                .eventId(attempt.getEventId())
                .endpointId(attempt.getEndpointId())
                .attemptNumber(attempt.getAttemptNumber())
                .requestUrl(attempt.getRequestUrl())
                .requestMethod(attempt.getRequestMethod())
                .requestHeaders(attempt.getRequestHeaders())
                .requestBody(attempt.getRequestBody())
                .responseStatus(attempt.getResponseStatus())
                .responseBody(attempt.getResponseBody())
                .responseHeaders(attempt.getResponseHeaders())
                .durationMs(attempt.getDurationMs())
                .errorMessage(attempt.getErrorMessage())
                .status(attempt.getStatus())
                .attemptedAt(attempt.getCreatedAt())
                .completedAt(attempt.getCompletedAt())
                .build();
    }
}