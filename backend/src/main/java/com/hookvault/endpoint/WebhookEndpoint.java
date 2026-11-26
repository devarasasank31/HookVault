package com.hookvault.endpoint;

import com.hookvault.common.BaseEntity;
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
@Table(name = "webhook_endpoints", indexes = {
        @Index(name = "idx_webhook_endpoints_user_id", columnList = "user_id"),
        @Index(name = "idx_webhook_endpoints_active", columnList = "is_active")
})
@Getter
@Setter
@Builder
public class WebhookEndpoint extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "signing_secret", nullable = false, length = 255)
    private String signingSecret;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "headers", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> headers = Map.of();

    // Helper methods
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }
}