package com.hookvault.delivery.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DeliveryResult {
    private boolean success;
    private int statusCode;
    private String body;
    private Map<String, String> headers;
    private long durationMs;
    private String errorMessage;

    public static DeliveryResult success(int statusCode, String body, Map<String, String> headers, long durationMs) {
        return DeliveryResult.builder()
                .success(true)
                .statusCode(statusCode)
                .body(body)
                .headers(headers)
                .durationMs(durationMs)
                .build();
    }

    public static DeliveryResult failure(int statusCode, String body, Map<String, String> headers, long durationMs, String errorMessage) {
        return DeliveryResult.builder()
                .success(false)
                .statusCode(statusCode)
                .body(body)
                .headers(headers)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();
    }

    public static DeliveryResult error(String errorMessage, long durationMs) {
        return DeliveryResult.builder()
                .success(false)
                .statusCode(0)
                .body(null)
                .headers(Map.of())
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();
    }
}