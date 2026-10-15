package io.github.gothsins.hookforge.endpoint;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "webhook_endpoints")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "owner_id")
    private UUID ownerId;

    protected WebhookEndpoint(String name, String url, UUID ownerId) {
        this.name = name;
        this.url = url;
        this.ownerId = Objects.requireNonNull(ownerId);
        this.active = true;
        this.createdAt = Instant.now();
    }

    public static WebhookEndpoint create(String name, String url, UUID ownerId) {
        return new WebhookEndpoint(name.trim(), url.trim(), ownerId);
    }
}
