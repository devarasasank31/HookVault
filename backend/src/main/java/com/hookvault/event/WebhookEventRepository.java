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

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Page<WebhookEvent> findByUserId(UUID userId, Pageable pageable);

    Page<WebhookEvent> findByEndpointId(UUID endpointId, Pageable pageable);

    Page<WebhookEvent> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

    Page<WebhookEvent> findByEndpointIdAndStatus(UUID endpointId, String status, Pageable pageable);

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

    long countByUserIdAndStatus(UUID userId, String status);

    long countByEndpointIdAndStatus(UUID endpointId, String status);

    long countByUserId(UUID userId);

    @Query("SELECT e FROM WebhookEvent e WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
    List<WebhookEvent> findRecentEventsByUserId(@Param("userId") UUID userId, @Param("limit") int limit);

    long countByUserIdAndStatusAndCreatedAtBetween(UUID userId, String status, Instant from, Instant to);

    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.user.id = :userId AND e.createdAt BETWEEN :from AND :to")
    long countByUserIdAndCreatedAtBetween(@Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);
}