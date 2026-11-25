package com.hookvault.auth;

import com.hookvault.auth.dto.AuthResponse;
import com.hookvault.auth.dto.LoginRequest;
import com.hookvault.auth.dto.RegisterRequest;
import com.hookvault.exception.ConflictException;
import com.hookvault.security.JwtService;
import com.hookvault.security.UserDetailsServiceImpl;
import com.hookvault.security.UserPrincipal;
import com.hookvault.user.User;
import com.hookvault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                authenticationManager,
                jwtService,
                userDetailsService,
                passwordEncoder
        );
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed-password")
                .fullName("Test User")
                .active(true)
                .emailVerified(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserPrincipal principal = UserPrincipal.create(savedUser);
        when(jwtService.generateAccessToken(principal)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(principal)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowConflictExceptionWhenEmailExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void login_shouldAuthenticateAndReturnTokens() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed-password")
                .active(true)
                .build();

        UserPrincipal principal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateAccessToken(principal)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(principal)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refreshToken_shouldReturnNewTokens() {
        // Given
        String refreshToken = "valid-refresh-token";
        String email = "test@example.com";

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .active(true)
                .build();

        UserPrincipal principal = UserPrincipal.create(user);

        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(principal);
        when(jwtService.isTokenValid(refreshToken, principal)).thenReturn(true);
        when(jwtService.generateAccessToken(principal)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(principal)).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);

        // When
        AuthResponse response = authService.refreshToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refreshToken_shouldThrowWhenNotRefreshToken() {
        // Given
        String accessToken = "access-token";
        when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> authService.refreshToken(accessToken))
                .isInstanceOf(com.hookvault.exception.UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}