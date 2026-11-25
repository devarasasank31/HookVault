package com.hookvault.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EndpointPerformanceResponse {
    private UUID endpointId;
    private String endpointName;
    private long totalEvents;
    private long delivered;
    private long failed;
    private long pending;
    private long deadLettered;
    private double successRate;
    private long averageResponseTimeMs;
}