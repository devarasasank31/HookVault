package com.hookvault.event;

import com.hookvault.event.dto.EventResponse;
import com.hookvault.event.dto.IngestEventRequest;
import com.hookvault.common.ApiResponse;
import com.hookvault.security.UserPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> ingestEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody IngestEventRequest request
    ) {
        EventResponse response = eventService.ingestEvent(principal, request);
        return ResponseEntity
                .status(201)
                .body(ApiResponse.success(response, "Event ingested successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> listEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String endpointId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<EventResponse> events = eventService.listEvents(principal, endpointId, eventType, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        EventResponse response = eventService.getEvent(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}