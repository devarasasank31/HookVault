package com.hookvault.delivery;

import com.hookvault.delivery.dto.DeliveryAttemptResponse;
import com.hookvault.common.ApiResponse;
import com.hookvault.security.UserPrincipal;
import com.hookvault.event.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/delivery-attempts")
@RequiredArgsConstructor
@Slf4j
public class DeliveryAttemptController {

    private final DeliveryAttemptRepository attemptRepository;
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeliveryAttemptResponse>>> listAttempts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String endpointId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "attemptedAt") Pageable pageable
    ) {
        Page<DeliveryAttempt> attempts;

        if (eventId != null) {
            UUID eventUuid = UUID.fromString(eventId);
            // Verify event ownership
            eventService.getEvent(principal, eventUuid);
            attempts = attemptRepository.findByEventId(eventUuid, pageable);
        } else if (endpointId != null) {
            UUID endpointUuid = UUID.fromString(endpointId);
            attempts = attemptRepository.findByEndpointId(endpointUuid, pageable);
        } else {
            // This would require a custom query to filter by user
            // For now, return empty page
            attempts = Page.empty(pageable);
        }

        Page<DeliveryAttemptResponse> response = attempts.map(DeliveryAttemptResponse::from);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryAttemptResponse>> getAttempt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        DeliveryAttempt attempt = attemptRepository.findById(id)
                .orElseThrow(() -> new com.hookvault.exception.ResourceNotFoundException("Delivery Attempt", id));

        // Verify ownership through event
        eventService.getEvent(principal, attempt.getEventId());

        return ResponseEntity.ok(ApiResponse.success(DeliveryAttemptResponse.from(attempt)));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<Page<DeliveryAttemptResponse>>> getAttemptsForEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID eventId,
            @PageableDefault(size = 20, sort = "attemptNumber") Pageable pageable
    ) {
        // Verify event ownership
        eventService.getEvent(principal, eventId);

        Page<DeliveryAttempt> attempts = attemptRepository.findByEventId(eventId, pageable);
        Page<DeliveryAttemptResponse> response = attempts.map(DeliveryAttemptResponse::from);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}