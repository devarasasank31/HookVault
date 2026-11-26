package com.hookvault.delivery;

import com.hookvault.delivery.dto.DeliveryResult;
import com.hookvault.endpoint.WebhookEndpoint;
import com.hookvault.event.WebhookEvent;
import com.hookvault.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpDeliveryClient {

    @Value("${hookvault.webhook.delivery.timeout:10000}")
    private int requestTimeoutMs;

    @Value("${hookvault.webhook.delivery.connect-timeout:5000}")
    private int connectTimeoutMs;

    @Value("${hookvault.webhook.ssrf.blocked-ips:127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,169.254.169.254}")
    private List<String> blockedIps;

    @Value("${hookvault.webhook.ssrf.allowed-schemes:https}")
    private List<String> allowedSchemes;

    @Value("${hookvault.webhook.ssrf.follow-redirects:false}")
    private boolean followRedirects;

    public DeliveryResult deliver(WebhookEndpoint endpoint, WebhookEvent event, int attemptNumber) {
        URI uri;
        try {
            uri = new URI(endpoint.getUrl());
        } catch (Exception e) {
            return DeliveryResult.error("Invalid URL: " + e.getMessage(), 0);
        }

        // SSRF Protection: Validate scheme
        if (!allowedSchemes.contains(uri.getScheme().toLowerCase())) {
            return DeliveryResult.error("URL scheme not allowed: " + uri.getScheme(), 0);
        }

        // SSRF Protection: Resolve and validate IP address
        String host = uri.getHost();
        if (host != null) {
            validateHost(host);
        }

        // Prepare request
        String payload;
        try {
            payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(event.getPayload());
        } catch (Exception e) {
            return DeliveryResult.error("Failed to serialize payload: " + e.getMessage(), 0);
        }

        // Create HTTP client with timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .setResponseTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
                .setRedirectsEnabled(followRedirects)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            // Add headers
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("X-HookVault-Event-ID", event.getId().toString());
            httpPost.setHeader("X-HookVault-Event-Type", event.getEventType());
            httpPost.setHeader("X-HookVault-Delivery-Attempt", String.valueOf(attemptNumber));
            httpPost.setHeader("User-Agent", "HookVault/1.0");

            // Add custom headers
            if (endpoint.getHeaders() != null) {
                endpoint.getHeaders().forEach(httpPost::setHeader);
            }

            long startTime = System.currentTimeMillis();
            try (ClassicHttpResponse response = httpClient.execute(httpPost)) {
                long durationMs = System.currentTimeMillis() - startTime;

                int statusCode = response.getCode();
                String responseBody = null;
                Map<String, String> responseHeaders = new HashMap<>();

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    responseBody = EntityUtils.toString(entity);
                }

                for (org.apache.hc.core5.http.Header header : response.getHeaders()) {
                    responseHeaders.put(header.getName(), header.getValue());
                }

                boolean success = statusCode >= 200 && statusCode < 300;

                if (success) {
                    return DeliveryResult.success(statusCode, responseBody, responseHeaders, durationMs);
                } else {
                    return DeliveryResult.failure(statusCode, responseBody, responseHeaders, durationMs,
                            "HTTP " + statusCode);
                }
            }

        } catch (IOException e) {
            long durationMs = System.currentTimeMillis() - (System.currentTimeMillis() - requestTimeoutMs); // approximate
            return DeliveryResult.error("Network error: " + e.getMessage(), durationMs);
        } catch (Exception e) {
            return DeliveryResult.error("Delivery error: " + e.getMessage(), 0);
        }
    }

    private void validateHost(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                String ip = address.getHostAddress();
                if (isBlockedIp(ip)) {
                    throw new ValidationException("Destination IP is blocked: " + ip);
                }
                // Block loopback, link-local, multicast
                if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isMulticastAddress()) {
                    throw new ValidationException("Destination IP type not allowed: " + ip);
                }
            }
        } catch (UnknownHostException e) {
            throw new ValidationException("Failed to resolve host: " + host);
        }
    }

    private boolean isBlockedIp(String ip) {
        for (String blocked : blockedIps) {
            if (blocked.contains("/")) {
                // CIDR notation
                if (ipMatchesCidr(ip, blocked)) {
                    return true;
                }
            } else {
                // Exact match
                if (ip.equals(blocked)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean ipMatchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String baseIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            InetAddress ipAddr = InetAddress.getByName(ip);
            InetAddress baseAddr = InetAddress.getByName(baseIp);

            byte[] ipBytes = ipAddr.getAddress();
            byte[] baseBytes = baseAddr.getAddress();

            if (ipBytes.length != baseBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != baseBytes[i]) {
                    return false;
                }
            }

            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                if ((ipBytes[fullBytes] & mask) != (baseBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("Failed to parse CIDR {}: {}", cidr, e.getMessage());
            return false;
        }
    }
}