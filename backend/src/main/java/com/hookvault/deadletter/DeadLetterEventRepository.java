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

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {

    Page<DeadLetterEvent> findByUserId(UUID userId, Pageable pageable);

    Page<DeadLetterEvent> findByEndpointId(UUID endpointId, Pageable pageable);

    Page<DeadLetterEvent> findByUserIdAndIsResolvedFalse(UUID userId, Pageable pageable);

    List<DeadLetterEvent> findByUserIdAndIsResolvedFalse(UUID userId);

    Optional<DeadLetterEvent> findByEventId(UUID eventId);

    Optional<DeadLetterEvent> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndIsResolvedFalse(UUID userId);

    long countByEndpointIdAndIsResolvedFalse(UUID endpointId);
}