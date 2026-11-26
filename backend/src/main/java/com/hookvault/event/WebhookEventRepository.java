package com.hookvault.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WebhookEvent exposes helper getters such as {@code getUserId()} and
 * {@code getEndpointId()} which win over the association properties during
 * Spring Data property resolution, so every derived query built from those
 * names would fail with "Could not resolve attribute 'userId'". Each of these
 * queries is therefore written explicitly against the associations.
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    @Query("SELECT e FROM WebhookEvent e WHERE e.user.id = :userId")
    Page<WebhookEvent> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT e FROM WebhookEvent e WHERE e.endpoint.id = :endpointId")
    Page<WebhookEvent> findByEndpointId(@Param("endpointId") UUID endpointId, Pageable pageable);

    @Query("SELECT e FROM WebhookEvent e WHERE e.user.id = :userId AND e.status = :status")
    Page<WebhookEvent> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT e FROM WebhookEvent e WHERE e.endpoint.id = :endpointId AND e.status = :status")
    Page<WebhookEvent> findByEndpointIdAndStatus(
            @Param("endpointId") UUID endpointId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT e FROM WebhookEvent e WHERE e.user.id = :userId AND (:eventType IS NULL OR e.eventType = :eventType) AND (:status IS NULL OR e.status = :status) ORDER BY e.createdAt DESC")
    Page<WebhookEvent> findByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("eventType") String eventType,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT e FROM WebhookEvent e WHERE e.endpoint.id = :endpointId AND (:eventType IS NULL OR e.eventType = :eventType) AND (:status IS NULL OR e.status = :status) ORDER BY e.createdAt DESC")
    Page<WebhookEvent> findByEndpointIdWithFilters(
            @Param("endpointId") UUID endpointId,
            @Param("eventType") String eventType,
            @Param("status") String status,
            Pageable pageable
    );

    List<WebhookEvent> findByStatusAndCreatedAtBefore(String status, Instant before);

    List<WebhookEvent> findByStatusInAndCreatedAtAfter(List<String> statuses, Instant after);

    Optional<WebhookEvent> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT e FROM WebhookEvent e WHERE e.id = :id AND e.user.id = :userId")
    Optional<WebhookEvent> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.user.id = :userId AND e.status = :status")
    long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.endpoint.id = :endpointId AND e.status = :status")
    long countByEndpointIdAndStatus(@Param("endpointId") UUID endpointId, @Param("status") String status);

    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.endpoint.id = :endpointId")
    long countByEndpointId(@Param("endpointId") UUID endpointId);

    @Query("SELECT e FROM WebhookEvent e WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
    List<WebhookEvent> findRecentEventsByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.user.id = :userId AND e.status = :status AND e.createdAt BETWEEN :from AND :to")
    long countByUserIdAndStatusAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.user.id = :userId AND e.createdAt BETWEEN :from AND :to")
    long countByUserIdAndCreatedAtBetween(@Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);
}
