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

/**
 * DeliveryAttempt exposes helper getters (getEventId, getEndpointId) that
 * shadow the association properties during Spring Data property resolution,
 * so these queries are written explicitly against the associations.
 */
@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.event.id = :eventId")
    Page<DeliveryAttempt> findByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.event.id = :eventId ORDER BY a.attemptNumber ASC")
    List<DeliveryAttempt> findByEventIdOrderByAttemptNumberAsc(@Param("eventId") UUID eventId);

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.event.id = :eventId AND a.attemptNumber = :attemptNumber")
    Optional<DeliveryAttempt> findByEventIdAndAttemptNumber(
            @Param("eventId") UUID eventId,
            @Param("attemptNumber") int attemptNumber
    );

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.endpoint.id = :endpointId")
    Page<DeliveryAttempt> findByEndpointId(@Param("endpointId") UUID endpointId, Pageable pageable);

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.endpoint.id = :endpointId AND a.status = :status ORDER BY a.createdAt DESC")
    Page<DeliveryAttempt> findByEndpointIdAndStatus(
            @Param("endpointId") UUID endpointId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT COUNT(a) FROM DeliveryAttempt a WHERE a.event.id = :eventId")
    long countByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT COUNT(a) FROM DeliveryAttempt a WHERE a.endpoint.id = :endpointId AND a.status = :status")
    long countByEndpointIdAndStatus(@Param("endpointId") UUID endpointId, @Param("status") String status);

    @Query("SELECT a FROM DeliveryAttempt a WHERE a.event.id = :eventId ORDER BY a.attemptNumber DESC")
    Optional<DeliveryAttempt> findLatestByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT COUNT(a) FROM DeliveryAttempt a WHERE a.event.user.id = :userId AND a.attemptNumber > :attemptNumber")
    long countByUserIdAndAttemptNumberGreaterThan(
            @Param("userId") UUID userId,
            @Param("attemptNumber") int attemptNumber
    );

    @Query("SELECT AVG(a.durationMs) FROM DeliveryAttempt a WHERE a.event.user.id = :userId AND a.durationMs IS NOT NULL AND a.status = 'SUCCESS'")
    Double getAverageResponseTimeByUserId(@Param("userId") UUID userId);

    @Query("SELECT AVG(a.durationMs) FROM DeliveryAttempt a WHERE a.endpoint.id = :endpointId AND a.durationMs IS NOT NULL AND a.status = 'SUCCESS'")
    Double getAverageResponseTimeByEndpointId(@Param("endpointId") UUID endpointId);
}
