package com.hookvault.delivery;

import com.hookvault.event.WebhookEvent;
import com.hookvault.event.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliverySchedulerImpl implements DeliveryScheduler {

    private final TaskScheduler taskScheduler;
    private final WebhookEventRepository eventRepository;
    private final DeliveryService deliveryService;

    // Track scheduled tasks to avoid duplicates
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public void scheduleDelivery(UUID eventId) {
        // Cancel any existing scheduled task for this event
        cancelScheduledTask(eventId);

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    try {
                        deliveryService.deliverEvent(eventId);
                    } catch (Exception e) {
                        log.error("Scheduled delivery failed for event {}: {}", eventId, e.getMessage());
                    }
                },
                Instant.now()
        );

        scheduledTasks.put(eventId, future);
        log.debug("Scheduled immediate delivery for event {}", eventId);
    }

    @Override
    public void scheduleRetry(UUID eventId, int attemptNumber) {
        // Get retry delay based on attempt number
        long delaySeconds = getRetryDelay(attemptNumber);
        Instant runAt = Instant.now().plusSeconds(delaySeconds);

        // Cancel any existing scheduled task for this event
        cancelScheduledTask(eventId);

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    try {
                        deliveryService.deliverEvent(eventId);
                    } catch (Exception e) {
                        log.error("Scheduled retry failed for event {}: {}", eventId, e.getMessage());
                    }
                },
                runAt
        );

        scheduledTasks.put(eventId, future);
        log.debug("Scheduled retry {} for event {} at {}", attemptNumber, eventId, runAt);
    }

    private void cancelScheduledTask(UUID eventId) {
        ScheduledFuture<?> existing = scheduledTasks.remove(eventId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private long getRetryDelay(int attemptNumber) {
        // Default retry schedule: 5s, 30s, 2m, 10m, 30m
        return switch (attemptNumber) {
            case 1 -> 5;
            case 2 -> 30;
            case 3 -> 120;
            case 4 -> 600;
            case 5 -> 1800;
            default -> 3600; // 1 hour for any additional attempts
        };
    }
}