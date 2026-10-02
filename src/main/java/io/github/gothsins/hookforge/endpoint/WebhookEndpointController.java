package io.github.gothsins.hookforge.endpoint;

import io.github.gothsins.hookforge.endpoint.dto.CreateWebhookEndpointRequest;
import io.github.gothsins.hookforge.endpoint.dto.WebhookEndpointResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/endpoints")
@RequiredArgsConstructor
public class WebhookEndpointController {

    private final WebhookEndpointService service;

    @PostMapping
    public ResponseEntity<WebhookEndpointResponse> create(
            @Valid @RequestBody CreateWebhookEndpointRequest request) {

        WebhookEndpointResponse response = service.create(request);

        URI location = URI.create(
                "/api/v1/endpoints/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<WebhookEndpointResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookEndpointResponse> findById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(service.findById(id));
    }
}