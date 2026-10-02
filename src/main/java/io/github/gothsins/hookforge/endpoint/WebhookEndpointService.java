package io.github.gothsins.hookforge.endpoint;

import io.github.gothsins.hookforge.endpoint.dto.CreateWebhookEndpointRequest;
import io.github.gothsins.hookforge.endpoint.dto.WebhookEndpointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebhookEndpointService {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final WebhookEndpointRepository repository;

    public WebhookEndpointResponse create(CreateWebhookEndpointRequest request) {
        validateUrl(request.url());

        WebhookEndpoint endpoint =
                WebhookEndpoint.create(request.name(), request.url());

        WebhookEndpoint saved = repository.save(endpoint);

        return WebhookEndpointResponse.from(saved);
    }

    public List<WebhookEndpointResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(WebhookEndpointResponse::from)
                .toList();
    }

    public WebhookEndpointResponse findById(UUID id) {
        WebhookEndpoint endpoint = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Webhook endpoint not found"
                ));

        return WebhookEndpointResponse.from(endpoint);
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value);

            if (uri.getScheme() == null
                    || !ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase())
                    || uri.getHost() == null) {

                throw new IllegalArgumentException();
            }

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook URL must be a valid HTTP or HTTPS URL"
            );
        }
    }
}