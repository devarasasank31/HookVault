package com.hookvault.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@Slf4j
public class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate HMAC signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    public boolean verify(String payload, String secret, String signature) {
        if (payload == null || secret == null || secret.isEmpty()
                || signature == null || signature.isEmpty()) {
            return false;
        }
        String expectedSignature = sign(payload, secret);
        return constantTimeEquals(expectedSignature, signature);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}