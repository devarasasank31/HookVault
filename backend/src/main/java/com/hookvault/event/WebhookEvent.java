package com.hookvault.event;

import com.hookvault.common.BaseEntity;
import com.hookvault.endpoint.WebhookEndpoint;
import com.hookvault.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "webhook_events", indexes = {
        @Index(name = "idx_webhook_events_endpoint_id", columnList = "endpoint_id"),
        @Index(name = "idx_webhook_events_user_id", columnList = "user_id"),
        @Index(name = "idx_webhook_events_status", columnList = "status"),
        @Index(name = "idx_webhook_events_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
public class WebhookEvent extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", columnDefinition = "JSONB", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "delivered_at")
    private java.time.Instant deliveredAt;

    // Helper methods
    public UUID getEndpointId() {
        return endpoint != null ? endpoint.getId() : null;
    }

    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isDelivered() {
        return "DELIVERED".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isDeadLettered() {
        return "DEAD_LETTERED".equals(status);
    }
}