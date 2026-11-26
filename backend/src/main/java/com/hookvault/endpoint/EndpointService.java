package com.hookvault.endpoint;

import com.hookvault.endpoint.dto.CreateEndpointRequest;
import com.hookvault.endpoint.dto.EndpointResponse;
import com.hookvault.endpoint.dto.UpdateEndpointRequest;
import com.hookvault.exception.ConflictException;
import com.hookvault.exception.ForbiddenException;
import com.hookvault.exception.ResourceNotFoundException;
import com.hookvault.exception.ValidationException;
import com.hookvault.security.UserPrincipal;
import com.hookvault.user.User;
import com.hookvault.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final WebhookEndpointRepository endpointRepository;
    private final UserRepository userRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public EndpointResponse createEndpoint(UserPrincipal principal, CreateEndpointRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        if (endpointRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new ConflictException("Endpoint with this name already exists");
        }

        validateUrl(request.getUrl());

        String signingSecret = generateSigningSecret();

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .user(user)
                .name(request.getName())
                .url(request.getUrl())
                .signingSecret(signingSecret)
                .description(request.getDescription())
                .headers(request.getHeaders() != null ? request.getHeaders() : Map.of())
                .active(true)
                .build();

        endpoint = endpointRepository.save(endpoint);
        log.info("Created endpoint {} for user {}", endpoint.getId(), user.getId());

        return EndpointResponse.from(endpoint, signingSecret);
    }

    @Transactional(readOnly = true)
    public EndpointResponse getEndpoint(UserPrincipal principal, UUID endpointId) {
        WebhookEndpoint endpoint = endpointRepository.findByIdAndUserId(endpointId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        // Never expose signing secret in GET responses
        return EndpointResponse.from(endpoint);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<EndpointResponse> listEndpoints(
            UserPrincipal principal,
            org.springframework.data.domain.Pageable pageable
    ) {
        return endpointRepository.findByUserId(principal.getId(), pageable)
                .map(EndpointResponse::from);
    }

    @Transactional
    public EndpointResponse updateEndpoint(UserPrincipal principal, UUID endpointId, UpdateEndpointRequest request) {
        WebhookEndpoint endpoint = endpointRepository.findByIdAndUserId(endpointId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        if (request.getName() != null && !request.getName().equals(endpoint.getName())) {
            if (endpointRepository.existsByUserIdAndName(principal.getId(), request.getName())) {
                throw new ConflictException("Endpoint with this name already exists");
            }
            endpoint.setName(request.getName());
        }

        if (request.getUrl() != null && !request.getUrl().equals(endpoint.getUrl())) {
            validateUrl(request.getUrl());
            endpoint.setUrl(request.getUrl());
        }

        if (request.getDescription() != null) {
            endpoint.setDescription(request.getDescription());
        }

        if (request.getHeaders() != null) {
            endpoint.setHeaders(request.getHeaders());
        }

        if (request.getActive() != null) {
            endpoint.setActive(request.getActive());
        }

        endpoint = endpointRepository.save(endpoint);
        log.info("Updated endpoint {} for user {}", endpoint.getId(), principal.getId());

        return EndpointResponse.from(endpoint);
    }

    @Transactional
    public String rotateSigningSecret(UserPrincipal principal, UUID endpointId) {
        WebhookEndpoint endpoint = endpointRepository.findByIdAndUserId(endpointId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        String newSecret = generateSigningSecret();
        endpoint.setSigningSecret(newSecret);
        endpointRepository.save(endpoint);

        log.info("Rotated signing secret for endpoint {} by user {}", endpointId, principal.getId());
        return newSecret;
    }

    @Transactional
    public void deleteEndpoint(UserPrincipal principal, UUID endpointId) {
        WebhookEndpoint endpoint = endpointRepository.findByIdAndUserId(endpointId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        endpointRepository.delete(endpoint);
        log.info("Deleted endpoint {} for user {}", endpointId, principal.getId());
    }

    @Transactional(readOnly = true)
    public WebhookEndpoint getEndpointForDelivery(UUID endpointId) {
        return endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ValidationException("URL is required");
        }

        try {
            java.net.URI uri = new java.net.URI(url);
            if (uri.getScheme() == null) {
                throw new ValidationException("Invalid URL: scheme is missing");
            }
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                throw new ValidationException("Only HTTP and HTTPS URLs are allowed");
            }
            // Additional SSRF protection will be done at delivery time
        } catch (java.net.URISyntaxException e) {
            throw new ValidationException("Invalid URL format: " + e.getMessage());
        }
    }

    private String generateSigningSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}