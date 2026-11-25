package com.hookvault.deadletter;

import com.hookvault.deadletter.dto.DeadLetterResponse;
import com.hookvault.common.ApiResponse;
import com.hookvault.security.UserPrincipal;
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
@RequestMapping("/v1/dead-letters")
@RequiredArgsConstructor
@Slf4j
public class DeadLetterController {

    private final DeadLetterEventRepository deadLetterRepository;
    private final DeadLetterService deadLetterService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeadLetterResponse>>> listDeadLetters(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String endpointId,
            @RequestParam(required = false, defaultValue = "false") boolean resolved,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<DeadLetterEvent> deadLetters;

        if (endpointId != null) {
            UUID endpointUuid = UUID.fromString(endpointId);
            deadLetters = deadLetterRepository.findByEndpointId(endpointUuid, pageable);
        } else if (resolved) {
            deadLetters = deadLetterRepository.findByUserId(principal.getId(), pageable);
        } else {
            deadLetters = deadLetterRepository.findByUserIdAndIsResolvedFalse(principal.getId(), pageable);
        }

        Page<DeadLetterResponse> response = deadLetters.map(DeadLetterResponse::from);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeadLetterResponse>> getDeadLetter(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        DeadLetterEvent deadLetter = deadLetterRepository.findByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new com.hookvault.exception.ResourceNotFoundException("Dead Letter", id));

        return ResponseEntity.ok(ApiResponse.success(DeadLetterResponse.from(deadLetter)));
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<ApiResponse<DeadLetterResponse>> replayDeadLetter(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        DeadLetterResponse response = deadLetterService.replayDeadLetter(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response, "Event replayed successfully"));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<DeadLetterResponse>> resolveDeadLetter(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        DeadLetterResponse response = deadLetterService.resolveDeadLetter(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response, "Dead letter marked as resolved"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> dismissDeadLetter(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        deadLetterService.dismissDeadLetter(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Dead letter dismissed"));
    }
}