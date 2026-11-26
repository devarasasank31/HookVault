package com.hookvault.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * IdempotencyRecord exposes getUserId() and getEventId() helpers that shadow
 * the associations during Spring Data property resolution, so these queries
 * are written explicitly against the associations.
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    @Query("SELECT r FROM IdempotencyRecord r WHERE r.user.id = :userId AND r.idempotencyKey = :idempotencyKey")
    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(
            @Param("userId") UUID userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM IdempotencyRecord r WHERE r.user.id = :userId AND r.idempotencyKey = :idempotencyKey")
    boolean existsByUserIdAndIdempotencyKey(
            @Param("userId") UUID userId,
            @Param("idempotencyKey") String idempotencyKey
    );
}
