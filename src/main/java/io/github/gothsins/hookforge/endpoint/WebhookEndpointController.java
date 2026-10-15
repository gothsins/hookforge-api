package io.github.gothsins.hookforge.endpoint;

import io.github.gothsins.hookforge.endpoint.dto.CreateWebhookEndpointRequest;
import io.github.gothsins.hookforge.endpoint.dto.WebhookEndpointResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody CreateWebhookEndpointRequest request) {

        WebhookEndpointResponse response = service.create(ownerId, request);

        URI location = URI.create(
                "/api/v1/endpoints/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<WebhookEndpointResponse>> findAll(@AuthenticationPrincipal UUID ownerId) {
        return ResponseEntity.ok(service.findAll(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookEndpointResponse> findById(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID id) {

        return ResponseEntity.ok(service.findById(ownerId, id));
    }
}
