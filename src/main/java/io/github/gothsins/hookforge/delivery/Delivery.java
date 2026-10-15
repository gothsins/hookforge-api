package io.github.gothsins.hookforge.delivery;

import io.github.gothsins.hookforge.endpoint.WebhookEndpoint;
import io.github.gothsins.hookforge.event.Event;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "deliveries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "endpoint_id", nullable = false)
    private UUID endpointId;

    @Column(name = "endpoint_url", nullable = false, length = 2048)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Delivery(Event event, WebhookEndpoint endpoint, Instant now) {
        if (!event.getOwnerId().equals(endpoint.getOwnerId())) {
            throw new IllegalArgumentException("Event and endpoint must have the same owner");
        }
        this.ownerId = event.getOwnerId();
        this.eventId = event.getId();
        this.endpointId = endpoint.getId();
        this.endpointUrl = endpoint.getUrl();
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Delivery pending(Event event, WebhookEndpoint endpoint, Instant now) {
        return new Delivery(event, endpoint, now);
    }
}
