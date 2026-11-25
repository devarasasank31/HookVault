package com.hookvault.event;

import com.hookvault.event.dto.EventResponse;
import com.hookvault.event.dto.IngestEventRequest;
import com.hookvault.exception.ConflictException;
import com.hookvault.exception.ResourceNotFoundException;
import com.hookvault.exception.ValidationException;
import com.hookvault.security.UserPrincipal;
import com.hookvault.endpoint.WebhookEndpoint;
import com.hookvault.endpoint.WebhookEndpointRepository;
import com.hookvault.user.User;
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
class EventServiceTest {

    @Mock
    private WebhookEventRepository eventRepository;

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeliveryScheduler deliveryScheduler;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository,
                endpointRepository,
                idempotencyRepository,
                userRepository,
                deliveryScheduler
        );
    }

    @Test
    void ingestEvent_shouldCreateEventAndScheduleDelivery() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder().id(userId).active(true).build();

        User user = User.builder().id(userId).build();
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(user)
                .name("Test Endpoint")
                .url("https://example.com/webhook")
                .signingSecret("whsec_secret")
                .active(true)
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));
        when(idempotencyRepository.existsByUserIdAndIdempotencyKey(userId, "idem-key"))
                .thenReturn(false);
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(idempotencyRepository.save(any(IdempotencyRecord.class))).thenReturn(null);

        IngestEventRequest request = new IngestEventRequest();
        request.setEventType("payment.succeeded");
        request.setEndpointId(endpointId);
        request.setPayload(Map.of("amount", 1000));
        request.setIdempotencyKey("idem-key");

        // When
        EventResponse response = eventService.ingestEvent(principal, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo("payment.succeeded");
        assertThat(response.getEndpointId()).isEqualTo(endpointId);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getIdempotencyKey()).isEqualTo("idem-key");
        verify(eventRepository).save(any(WebhookEvent.class));
        verify(idempotencyRepository).save(any(IdempotencyRecord.class));
        verify(deliveryScheduler).scheduleDelivery(any(UUID.class));
    }

    @Test
    void ingestEvent_shouldReturnExistingEventForDuplicateIdempotencyKey() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder().id(userId).active(true).build();

        User user = User.builder().id(userId).build();
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(user)
                .active(true)
                .build();

        WebhookEvent existingEvent = WebhookEvent.builder()
                .id(eventId)
                .endpoint(endpoint)
                .user(user)
                .eventType("payment.succeeded")
                .payload(Map.of("amount", 1000))
                .idempotencyKey("idem-key")
                .status("DELIVERED")
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));
        when(idempotencyRepository.existsByUserIdAndIdempotencyKey(userId, "idem-key"))
                .thenReturn(true);
        when(eventRepository.findByIdempotencyKey("idem-key"))
                .thenReturn(Optional.of(existingEvent));

        IngestEventRequest request = new IngestEventRequest();
        request.setEventType("payment.succeeded");
        request.setEndpointId(endpointId);
        request.setPayload(Map.of("amount", 2000)); // Different payload
        request.setIdempotencyKey("idem-key");

        // When
        EventResponse response = eventService.ingestEvent(principal, request);

        // Then
        assertThat(response.getId()).isEqualTo(eventId);
        assertThat(response.getStatus()).isEqualTo("DELIVERED");
        verify(eventRepository, never()).save(any(WebhookEvent.class));
    }

    @Test
    void ingestEvent_shouldThrowWhenEndpointNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder().id(userId).active(true).build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.empty());

        IngestEventRequest request = new IngestEventRequest();
        request.setEventType("payment.succeeded");
        request.setEndpointId(endpointId);
        request.setPayload(Map.of());

        // When / Then
        assertThatThrownBy(() -> eventService.ingestEvent(principal, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ingestEvent_shouldThrowWhenEndpointInactive() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder().id(userId).active(true).build();

        User user = User.builder().id(userId).build();
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(user)
                .active(false) // Inactive
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));

        IngestEventRequest request = new IngestEventRequest();
        request.setEventType("payment.succeeded");
        request.setEndpointId(endpointId);
        request.setPayload(Map.of());

        // When / Then
        assertThatThrownBy(() -> eventService.ingestEvent(principal, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void ingestEvent_shouldWorkWithoutIdempotencyKey() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID endpointId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder().id(userId).active(true).build();

        User user = User.builder().id(userId).build();
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .id(endpointId)
                .user(user)
                .active(true)
                .build();

        when(endpointRepository.findByIdAndUserId(endpointId, userId))
                .thenReturn(Optional.of(endpoint));
        when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        IngestEventRequest request = new IngestEventRequest();
        request.setEventType("payment.succeeded");
        request.setEndpointId(endpointId);
        request.setPayload(Map.of("amount", 1000));
        request.setIdempotencyKey(null);

        // When
        EventResponse response = eventService.ingestEvent(principal, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIdempotencyKey()).isNull();
        verify(idempotencyRepository, never()).save(any(IdempotencyRecord.class));
    }
}