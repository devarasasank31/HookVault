package com.hookvault.analytics;

import com.hookvault.analytics.dto.AnalyticsDashboardResponse;
import com.hookvault.analytics.dto.EndpointPerformanceResponse;
import com.hookvault.analytics.dto.VolumeOverTimeResponse;
import com.hookvault.event.WebhookEventRepository;
import com.hookvault.endpoint.WebhookEndpointRepository;
import com.hookvault.delivery.DeliveryAttemptRepository;
import com.hookvault.deadletter.DeadLetterEventRepository;
import com.hookvault.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final WebhookEventRepository eventRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final DeadLetterEventRepository deadLetterRepository;

    @Transactional(readOnly = true)
    public AnalyticsDashboardResponse getDashboard(UserPrincipal principal) {
        UUID userId = principal.getId();
        Instant now = Instant.now();
        Instant dayAgo = now.minus(1, ChronoUnit.DAYS);
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant monthAgo = now.minus(30, ChronoUnit.DAYS);

        // Total events
        long totalEvents = eventRepository.countByUserId(userId);
        long totalDelivered = eventRepository.countByUserIdAndStatus(userId, "DELIVERED");
        long totalFailed = eventRepository.countByUserIdAndStatus(userId, "FAILED");
        long totalPending = eventRepository.countByUserIdAndStatus(userId, "PENDING");
        long totalDeadLettered = eventRepository.countByUserIdAndStatus(userId, "DEAD_LETTERED");
        long totalRetries = attemptRepository.countByUserIdAndAttemptNumberGreaterThan(userId, 1);

        double successRate = totalEvents > 0 ? (double) totalDelivered / totalEvents * 100 : 0;

        // Average response time
        Double avgResponseTime = attemptRepository.getAverageResponseTimeByUserId(userId);

        // Recent events (last 10)
        List<RecentEventInfo> recentEvents = eventRepository.findRecentEventsByUserId(userId, 10)
                .stream()
                .map(e -> new RecentEventInfo(
                        e.getId(),
                        e.getEventType(),
                        e.getStatus(),
                        e.getEndpoint().getName(),
                        e.getCreatedAt()
                ))
                .toList();

        // Endpoint performance
        List<EndpointPerformanceResponse> endpointPerformance = getEndpointPerformance(userId);

        // Volume over time (last 7 days)
        List<VolumeOverTimeResponse> volumeOverTime = getVolumeOverTime(userId, weekAgo, now);

        return AnalyticsDashboardResponse.builder()
                .totalEvents(totalEvents)
                .totalDelivered(totalDelivered)
                .totalFailed(totalFailed)
                .totalPending(totalPending)
                .totalDeadLettered(totalDeadLettered)
                .totalRetries(totalRetries)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .averageResponseTimeMs(avgResponseTime != null ? Math.round(avgResponseTime) : 0L)
                .recentEvents(recentEvents)
                .endpointPerformance(endpointPerformance)
                .volumeOverTime(volumeOverTime)
                .build();
    }

    private List<EndpointPerformanceResponse> getEndpointPerformance(UUID userId) {
        List<WebhookEndpoint> endpoints = endpointRepository.findByUserIdAndIsActiveTrue(userId);

        return endpoints.stream().map(endpoint -> {
            long endpointEvents = eventRepository.countByEndpointId(endpoint.getId());
            long endpointDelivered = eventRepository.countByEndpointIdAndStatus(endpoint.getId(), "DELIVERED");
            long endpointFailed = eventRepository.countByEndpointIdAndStatus(endpoint.getId(), "FAILED");
            long endpointPending = eventRepository.countByEndpointIdAndStatus(endpoint.getId(), "PENDING");
            long endpointDeadLettered = eventRepository.countByEndpointIdAndStatus(endpoint.getId(), "DEAD_LETTERED");

            Double avgTime = attemptRepository.getAverageResponseTimeByEndpointId(endpoint.getId());

            double endpointSuccessRate = endpointEvents > 0 ? (double) endpointDelivered / endpointEvents * 100 : 0;

            return EndpointPerformanceResponse.builder()
                    .endpointId(endpoint.getId())
                    .endpointName(endpoint.getName())
                    .totalEvents(endpointEvents)
                    .delivered(endpointDelivered)
                    .failed(endpointFailed)
                    .pending(endpointPending)
                    .deadLettered(endpointDeadLettered)
                    .successRate(Math.round(endpointSuccessRate * 100.0) / 100.0)
                    .averageResponseTimeMs(avgTime != null ? Math.round(avgTime) : 0L)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<VolumeOverTimeResponse> getVolumeOverTime(UUID userId, Instant from, Instant to) {
        // This would typically be a custom query grouping by day/hour
        // For now, return mock structure
        List<VolumeOverTimeResponse> result = new ArrayList<>();
        Instant current = from.truncatedTo(ChronoUnit.DAYS);

        while (current.isBefore(to)) {
            Instant next = current.plus(1, ChronoUnit.DAYS);
            long count = eventRepository.countByUserIdAndCreatedAtBetween(userId, current, next);
            long delivered = eventRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, "DELIVERED", current, next);
            long failed = eventRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, "FAILED", current, next);

            result.add(VolumeOverTimeResponse.builder()
                    .date(current.toString().substring(0, 10))
                    .total(count)
                    .delivered(delivered)
                    .failed(failed)
                    .build());

            current = next;
        }

        return result;
    }

    // Helper record for recent events
    public record RecentEventInfo(
            UUID id,
            String eventType,
            String status,
            String endpointName,
            Instant createdAt
    ) {}
}