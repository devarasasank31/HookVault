package com.hookvault.endpoint;

import com.hookvault.endpoint.dto.CreateEndpointRequest;
import com.hookvault.endpoint.dto.EndpointResponse;
import com.hookvault.endpoint.dto.UpdateEndpointRequest;
import com.hookvault.common.ApiResponse;
import com.hookvault.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@RequestMapping("/v1/endpoints")
@RequiredArgsConstructor
@Slf4j
public class EndpointController {

    private final EndpointService endpointService;

    @PostMapping
    public ResponseEntity<ApiResponse<EndpointResponse>> createEndpoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEndpointRequest request
    ) {
        EndpointResponse response = endpointService.createEndpoint(principal, request);
        return ResponseEntity
                .status(201)
                .body(ApiResponse.success(response, "Endpoint created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EndpointResponse>>> listEndpoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<EndpointResponse> endpoints = endpointService.listEndpoints(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(endpoints));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EndpointResponse>> getEndpoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        EndpointResponse response = endpointService.getEndpoint(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<EndpointResponse>> updateEndpoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEndpointRequest request
    ) {
        EndpointResponse response = endpointService.updateEndpoint(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Endpoint updated successfully"));
    }

    @PostMapping("/{id}/rotate-secret")
    public ResponseEntity<ApiResponse<RotateSecretResponse>> rotateSigningSecret(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        String newSecret = endpointService.rotateSigningSecret(principal, id);
        return ResponseEntity.ok(ApiResponse.success(
                new RotateSecretResponse(newSecret),
                "Signing secret rotated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEndpoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        endpointService.deleteEndpoint(principal, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint deleted successfully"));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RotateSecretResponse {
        private String signingSecret;
    }
}