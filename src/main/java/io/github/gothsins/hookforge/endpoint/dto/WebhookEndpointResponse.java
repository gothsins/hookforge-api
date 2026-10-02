package io.github.gothsins.hookforge.endpoint.dto;

import io.github.gothsins.hookforge.endpoint.WebhookEndpoint;

import java.time.Instant;
import java.util.UUID;

public record WebhookEndpointResponse(
        UUID id,
        String name,
        String url,
        boolean active,
        Instant createdAt
) {

    public static WebhookEndpointResponse from(WebhookEndpoint endpoint) {
        return new WebhookEndpointResponse(
                endpoint.getId(),
                endpoint.getName(),
                endpoint.getUrl(),
                endpoint.isActive(),
                endpoint.getCreatedAt()
        );
    }
}