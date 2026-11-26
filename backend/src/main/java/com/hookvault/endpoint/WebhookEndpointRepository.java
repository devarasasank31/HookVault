package com.hookvault.endpoint;

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
 * WebhookEndpoint exposes a getUserId() helper that shadows the user
 * association during Spring Data property resolution, so these queries are
 * written explicitly against the association.
 */
@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.user.id = :userId")
    Page<WebhookEndpoint> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.user.id = :userId AND e.active = true")
    List<WebhookEndpoint> findByUserIdAndIsActiveTrue(@Param("userId") UUID userId);

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.id = :id AND e.user.id = :userId")
    Optional<WebhookEndpoint> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.id = :id AND e.user.id = :userId")
    Optional<WebhookEndpoint> findByIdAndUserIdExplicit(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM WebhookEndpoint e WHERE e.user.id = :userId AND e.name = :name")
    boolean existsByUserIdAndName(@Param("userId") UUID userId, @Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM WebhookEndpoint e WHERE e.id = :id AND e.user.id = :userId")
    boolean existsByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.active = true")
    List<WebhookEndpoint> findAllActive();
}
