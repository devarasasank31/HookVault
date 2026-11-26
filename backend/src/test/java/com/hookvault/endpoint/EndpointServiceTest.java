package com.hookvault.endpoint;

import com.hookvault.endpoint.dto.CreateEndpointRequest;
import com.hookvault.endpoint.dto.EndpointResponse;
import com.hookvault.endpoint.dto.UpdateEndpointRequest;
import com.hookvault.exception.ConflictException;
import com.hookvault.exception.ResourceNotFoundException;
import com.hookvault.exception.ValidationException;
import com.hookvault.security.UserPrincipal;
import com.hookvault.user.User;
import com.hookvault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointServiceTest {

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private UserRepository userRepository;

    private EndpointService endpointService;

    @BeforeEach
    void setUp() {
        endpointService = new EndpointService(endpointRepository, userRepository);
    }

    private UserPrincipal createPrincipal(UUID userId) {
        User user = User.builder().id(userId).email("test@example.com").passwordHash("hash").active(true).emailVerified(false).build();
        return UserPrincipal.create(user);
    }

    @Test
    void createEndpoint_shouldCreateAndReturnWithSecret() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);

        User user = User.builder().id(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(endpointRepository.existsByUserIdAndName(userId, "Test Endpoint")).thenReturn(false);

        CreateEndpointRequest request = new CreateEndpointRequest();
        request.setName("Test Endpoint");
        request.setUrl("https://example.com/webhook");
        request.setDescription("Test description");
        request.setHeaders(Map.of("X-Custom", "value"));

        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(inv -> {
            WebhookEndpoint e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // When
        EndpointResponse response = endpointService.createEndpoint(principal, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Endpoint");
        assertThat(response.getUrl()).isEqualTo("https://example.com/webhook");
        assertThat(response.getSigningSecret()).isNotNull();
        assertThat(response.getSigningSecret()).startsWith("whsec_");
        verify(endpointRepository).save(any(WebhookEndpoint.class));
    }

    @Test
    void createEndpoint_shouldThrowConflictWhenNameExists() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        User user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(endpointRepository.existsByUserIdAndName(userId, "Existing")).thenReturn(true);

        CreateEndpointRequest request = new CreateEndpointRequest();
        request.setName("Existing");
        request.setUrl("https://example.com/webhook");

        // When / Then
        assertThatThrownBy(() -> endpointService.createEndpoint(principal, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createEndpoint_shouldThrowValidationForInvalidUrl() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        User user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(endpointRepository.existsByUserIdAndName(any(), any())).thenReturn(false);

        CreateEndpointRequest request = new CreateEndpointRequest();
        request.setName("Test");
        request.setUrl("not-a-valid-url");

        // When / Then
        assertThatThrownBy(() -> endpointService.createEndpoint(principal, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid URL");
    }

    @Test
    void createEndpoint_shouldRejectNonHttpSchemes() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);
        User user = User.builder().id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(endpointRepository.existsByUserIdAndName(any(), any())).thenReturn(false);

        CreateEndpointRequest request = new CreateEndpointRequest();
        request.setName("Test");
        request.setUrl("ftp://example.com/file");

        // When / Then
        assertThatThrownBy(() -> endpointService.createEndpoint(principal, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getEndpoint_shouldReturnWithoutSecret() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(User.builder().id(userId).build())
                .name("Test")
                .url("https://example.com/webhook")
                .signingSecret("whsec_secret123")
                .active(true)
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));

        // When
        EndpointResponse response = endpointService.getEndpoint(principal, endpointId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSigningSecret()).isNull();
    }

    @Test
    void rotateSigningSecret_shouldGenerateNewSecret() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(User.builder().id(userId).build())
                .name("Test")
                .url("https://example.com/webhook")
                .signingSecret("whsec_oldsecret")
                .active(true)
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));
        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        String newSecret = endpointService.rotateSigningSecret(principal, endpointId);

        // Then
        assertThat(newSecret).isNotNull();
        assertThat(newSecret).startsWith("whsec_");
        assertThat(newSecret).isNotEqualTo("whsec_oldsecret");
        verify(endpointRepository).save(any(WebhookEndpoint.class));
    }

    @Test
    void deleteEndpoint_shouldDeleteWhenOwned() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(User.builder().id(userId).build())
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));

        // When
        endpointService.deleteEndpoint(principal, endpointId);

        // Then
        verify(endpointRepository).delete(endpoint);
    }

    @Test
    void updateEndpoint_shouldUpdateFields() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId);

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(User.builder().id(userId).build())
                .name("Old Name")
                .url("https://old.com/webhook")
                .signingSecret("whsec_secret")
                .active(true)
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));
        when(endpointRepository.existsByUserIdAndName(userId, "New Name")).thenReturn(false);
        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateEndpointRequest request = new UpdateEndpointRequest();
        request.setName("New Name");
        request.setUrl("https://new.com/webhook");
        request.setActive(false);

        // When
        EndpointResponse response = endpointService.updateEndpoint(principal, endpointId, request);

        // Then
        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getUrl()).isEqualTo("https://new.com/webhook");
        assertThat(response.isActive()).isFalse();
    }
}