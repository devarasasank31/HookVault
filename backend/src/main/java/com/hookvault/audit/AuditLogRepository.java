package com.hookvault.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * AuditLog exposes a getUserId() helper that shadows the user association
 * during Spring Data property resolution, so these queries are written
 * explicitly against the association.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId")
    Page<AuditLog> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
}
