package com.hookvault.deadletter;

import com.hookvault.common.BaseEntity;
import com.hookvault.endpoint.WebhookEndpoint;
import com.hookvault.event.WebhookEvent;
import com.hookvault.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_events", indexes = {
        @Index(name = "idx_dead_letter_events_endpoint_id", columnList = "endpoint_id"),
        @Index(name = "idx_dead_letter_events_user_id", columnList = "user_id"),
        @Index(name = "idx_dead_letter_events_resolved", columnList = "is_resolved"),
        @Index(name = "idx_dead_letter_events_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
public class DeadLetterEvent extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private WebhookEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    // Helper methods
    public UUID getEventId() {
        return event != null ? event.getId() : null;
    }

    public UUID getEndpointId() {
        return endpoint != null ? endpoint.getId() : null;
    }

    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    public UUID getResolvedById() {
        return resolvedBy != null ? resolvedBy.getId() : null;
    }

    public boolean isResolved() {
        return resolved;
    }
}