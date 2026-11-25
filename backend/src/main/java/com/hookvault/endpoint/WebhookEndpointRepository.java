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

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    Page<WebhookEndpoint> findByUserId(UUID userId, Pageable pageable);

    List<WebhookEndpoint> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<WebhookEndpoint> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.id = :id AND e.user.id = :userId")
    Optional<WebhookEndpoint> findByIdAndUserIdExplicit(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    @Query("SELECT e FROM WebhookEndpoint e WHERE e.isActive = true")
    List<WebhookEndpoint> findAllActive();
}