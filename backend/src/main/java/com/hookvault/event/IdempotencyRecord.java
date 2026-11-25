package com.hookvault.event;

import com.hookvault.common.BaseEntity;
import com.hookvault.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records", indexes = {
        @Index(name = "idx_idempotency_records_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@Builder
public class IdempotencyRecord extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private WebhookEvent event;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Unique constraint is handled in the migration

    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    public UUID getEventId() {
        return event != null ? event.getId() : null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}