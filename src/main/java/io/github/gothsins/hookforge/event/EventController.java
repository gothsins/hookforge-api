package io.github.gothsins.hookforge.event;

import io.github.gothsins.hookforge.event.dto.CreateEventRequest;
import io.github.gothsins.hookforge.event.dto.EventResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @AuthenticationPrincipal UUID ownerId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateEventRequest request
    ) {

        EventService.Creation result = service.create(ownerId, request, idempotencyKey);
        EventResponse response = result.response();

        URI location = URI.create(
                "/api/v1/events/" + response.id()
        );

        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> findAll(@AuthenticationPrincipal UUID ownerId) {
        return ResponseEntity.ok(service.findAll(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(service.findById(ownerId, id));
    }
}
