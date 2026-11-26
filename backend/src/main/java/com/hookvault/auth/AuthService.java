package com.hookvault.auth;

import com.hookvault.auth.dto.AuthResponse;
import com.hookvault.auth.dto.LoginRequest;
import com.hookvault.auth.dto.RegisterRequest;
import com.hookvault.exception.ConflictException;
import com.hookvault.exception.UnauthorizedException;
import com.hookvault.exception.ValidationException;
import com.hookvault.security.JwtService;
import com.hookvault.security.UserDetailsServiceImpl;
import com.hookvault.security.UserPrincipal;
import com.hookvault.user.User;
import com.hookvault.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User", request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .active(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        log.info("User logged in: {}", principal.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UserPrincipal principal = (UserPrincipal) userDetails;

        if (!jwtService.isTokenValid(refreshToken, principal)) {
            throw new UnauthorizedException("Refresh token expired or invalid");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(AuthResponse.UserInfo.from(user))
                .build();
    }
}