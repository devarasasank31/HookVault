package com.hookvault.deadletter;

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
 * DeadLetterEvent exposes helper getters (getUserId, getEndpointId, getEventId)
 * that shadow the association properties during Spring Data property resolution,
 * so these queries are written explicitly against the associations.
 */
@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {

    @Query("SELECT d FROM DeadLetterEvent d WHERE d.user.id = :userId")
    Page<DeadLetterEvent> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT d FROM DeadLetterEvent d WHERE d.endpoint.id = :endpointId")
    Page<DeadLetterEvent> findByEndpointId(@Param("endpointId") UUID endpointId, Pageable pageable);

    @Query("SELECT d FROM DeadLetterEvent d WHERE d.user.id = :userId AND d.resolved = false")
    Page<DeadLetterEvent> findByUserIdAndIsResolvedFalse(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT d FROM DeadLetterEvent d WHERE d.user.id = :userId AND d.resolved = false")
    List<DeadLetterEvent> findByUserIdAndIsResolvedFalse(@Param("userId") UUID userId);

    @Query("SELECT d FROM DeadLetterEvent d WHERE d.event.id = :eventId")
    Optional<DeadLetterEvent> findByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT d FROM DeadLetterEvent d WHERE d.id = :id AND d.user.id = :userId")
    Optional<DeadLetterEvent> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT COUNT(d) FROM DeadLetterEvent d WHERE d.user.id = :userId AND d.resolved = false")
    long countByUserIdAndIsResolvedFalse(@Param("userId") UUID userId);

    @Query("SELECT COUNT(d) FROM DeadLetterEvent d WHERE d.endpoint.id = :endpointId AND d.resolved = false")
    long countByEndpointIdAndIsResolvedFalse(@Param("endpointId") UUID endpointId);
}
