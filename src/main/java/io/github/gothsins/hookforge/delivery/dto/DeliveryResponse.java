package io.github.gothsins.hookforge.delivery.dto;

import io.github.gothsins.hookforge.delivery.Delivery;
import io.github.gothsins.hookforge.delivery.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID eventId,
        UUID endpointId,
        DeliveryStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        Instant createdAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(delivery.getId(), delivery.getEventId(), delivery.getEndpointId(),
                delivery.getStatus(), delivery.getAttemptCount(), delivery.getNextAttemptAt(), delivery.getCreatedAt());
    }
}
