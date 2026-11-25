package com.hookvault.delivery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    Page<DeliveryAttempt> findByEventId(UUID eventId, Pageable pageable);

    List<DeliveryAttempt> findByEventIdOrderByAttemptNumberAsc(UUID eventId);

    Optional<DeliveryAttempt> findByEventIdAndAttemptNumber(UUID eventId, int attemptNumber);

    Page<DeliveryAttempt> findByEndpointId(UUID endpointId, Pageable pageable);

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.endpoint.id = :endpointId AND a.status = :status ORDER BY a.attemptedAt DESC")
    Page<DeliveryAttempt> findByEndpointIdAndStatus(
            @Param("endpointId") UUID endpointId,
            @Param("status") String status,
            Pageable pageable
    );

    long countByEventId(UUID eventId);

    long countByEndpointIdAndStatus(UUID endpointId, String status);

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.event.id = :eventId ORDER BY a.attemptNumber DESC")
    Optional<DeliveryAttempt> findLatestByEventId(@Param("eventId") UUID eventId);

    long countByUserIdAndAttemptNumberGreaterThan(UUID userId, int attemptNumber);

    @Query("SELECT AVG(a.durationMs) FROM DeliveryAttempt a WHERE a.event.user.id = :userId AND a.durationMs IS NOT NULL AND a.status = 'SUCCESS'")
    Double getAverageResponseTimeByUserId(@Param("userId") UUID userId);

    @Query("SELECT AVG(a.durationMs) FROM DeliveryAttempt a WHERE a.endpoint.id = :endpointId AND a.durationMs IS NOT NULL AND a.status = 'SUCCESS'")
    Double getAverageResponseTimeByEndpointId(@Param("endpointId") UUID endpointId);
}