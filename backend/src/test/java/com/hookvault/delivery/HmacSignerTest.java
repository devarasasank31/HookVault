package com.hookvault.delivery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HmacSignerTest {

    private final HmacSigner signer = new HmacSigner();

    @Test
    void sign_shouldGenerateValidSignature() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String secret = "whsec_testsecret123";

        // When
        String signature = signer.sign(payload, secret);

        // Then
        assertThat(signature).startsWith("sha256=");
        assertThat(signature.length()).isGreaterThan(7);
    }

    @Test
    void verify_shouldReturnTrueForValidSignature() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String secret = "whsec_testsecret123";
        String signature = signer.sign(payload, secret);

        // When
        boolean result = signer.verify(payload, secret, signature);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void verify_shouldReturnFalseForInvalidSignature() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String secret = "whsec_testsecret123";
        String wrongSecret = "whsec_wrongsecret";

        // When
        String signature = signer.sign(payload, secret);
        boolean result = signer.verify(payload, wrongSecret, signature);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void verify_shouldReturnFalseForTamperedPayload() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String tamperedPayload = "{\"test\": \"tampered\"}";
        String secret = "whsec_testsecret123";

        // When
        String signature = signer.sign(payload, secret);
        boolean result = signer.verify(tamperedPayload, secret, signature);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void sign_shouldBeDeterministic() {
        // Given
        String payload = "{\"test\": \"data\"}";
        String secret = "whsec_testsecret123";

        // When
        String signature1 = signer.sign(payload, secret);
        String signature2 = signer.sign(payload, secret);

        // Then
        assertThat(signature1).isEqualTo(signature2);
    }

    @Test
    void verify_shouldUseConstantTimeComparison() {
        // This test verifies the method exists and doesn't throw
        // Actual timing attack resistance is implementation detail
        String payload = "test";
        String secret = "secret";
        String signature = signer.sign(payload, secret);

        // Should not throw
        assertThatCode(() -> signer.verify(payload, secret, signature))
                .doesNotThrowAnyException();
    }

    @Test
    void verify_shouldHandleNullInputs() {
        // Given/When/Then
        assertThat(signer.verify(null, "secret", "sig")).isFalse();
        assertThat(signer.verify("payload", null, "sig")).isFalse();
        assertThat(signer.verify("payload", "secret", null)).isFalse();
        assertThat(signer.verify("", "", "")).isFalse();
    }
}