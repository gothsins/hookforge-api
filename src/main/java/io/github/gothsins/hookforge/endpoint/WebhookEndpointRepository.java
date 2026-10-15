package io.github.gothsins.hookforge.endpoint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpointRepository
        extends JpaRepository<WebhookEndpoint, UUID> {
    List<WebhookEndpoint> findAllByOwnerId(UUID ownerId);
    List<WebhookEndpoint> findAllByOwnerIdAndActiveTrue(UUID ownerId);
    Optional<WebhookEndpoint> findByIdAndOwnerId(UUID id, UUID ownerId);
}
