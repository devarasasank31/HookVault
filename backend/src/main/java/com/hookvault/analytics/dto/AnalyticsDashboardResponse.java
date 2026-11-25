package com.hookvault.analytics.dto;

import com.hookvault.analytics.AnalyticsService;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalyticsDashboardResponse {
    private long totalEvents;
    private long totalDelivered;
    private long totalFailed;
    private long totalPending;
    private long totalDeadLettered;
    private long totalRetries;
    private double successRate;
    private long averageResponseTimeMs;
    private List<AnalyticsService.RecentEventInfo> recentEvents;
    private List<EndpointPerformanceResponse> endpointPerformance;
    private List<VolumeOverTimeResponse> volumeOverTime;
}