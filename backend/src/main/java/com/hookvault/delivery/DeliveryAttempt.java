package com.hookvault.delivery;

import com.hookvault.common.BaseEntity;
import com.hookvault.common.MapToJsonConverter;
import com.hookvault.endpoint.WebhookEndpoint;
import com.hookvault.event.WebhookEvent;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts", indexes = {
        @Index(name = "idx_delivery_attempts_event_id", columnList = "event_id"),
        @Index(name = "idx_delivery_attempts_endpoint_id", columnList = "endpoint_id"),
        @Index(name = "idx_delivery_attempts_status", columnList = "status"),
        @Index(name = "idx_delivery_attempts_attempted_at", columnList = "attempted_at")
})
@Getter
@Setter
@Builder
public class DeliveryAttempt extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private WebhookEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "request_url", nullable = false, length = 2048)
    private String requestUrl;

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod = "POST";

    @Column(name = "request_headers", columnDefinition = "JSONB")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> requestHeaders;

    @Column(name = "request_body", columnDefinition = "JSONB")
    private Map<String, Object> requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_headers", columnDefinition = "JSONB")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> responseHeaders;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "completed_at")
    private java.time.Instant completedAt;

    // Helper methods
    public UUID getEventId() {
        return event != null ? event.getId() : null;
    }

    public UUID getEndpointId() {
        return endpoint != null ? endpoint.getId() : null;
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public boolean isFailure() {
        return "FAILURE".equals(status);
    }

    public boolean isTimeout() {
        return "TIMEOUT".equals(status);
    }
}