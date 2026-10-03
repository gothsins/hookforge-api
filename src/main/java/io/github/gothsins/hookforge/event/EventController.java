package io.github.gothsins.hookforge.event;

import io.github.gothsins.hookforge.event.dto.CreateEventRequest;
import io.github.gothsins.hookforge.event.dto.EventResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @Valid @RequestBody CreateEventRequest request
    ) {

        EventResponse response = service.create(request);

        URI location = URI.create(
                "/api/v1/events/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(service.findById(id));
    }
}