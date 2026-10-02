package io.github.gothsins.hookforge.endpoint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEndpointRepository
        extends JpaRepository<WebhookEndpoint, UUID> {
}