package io.github.gothsins.hookforge.event;

import io.github.gothsins.hookforge.delivery.DeliveryService;
import io.github.gothsins.hookforge.event.dto.CreateEventRequest;
import io.github.gothsins.hookforge.event.dto.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    private final EventRepository repository;
    private final DeliveryService deliveryService;
    private final JdbcTemplate jdbc;

    public record Creation(EventResponse response, boolean replayed) {}

    @Transactional
    public Creation create(UUID ownerId, CreateEventRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            if (!IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid idempotency key");
            }
            jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?))",
                    statement -> statement.setString(1, ownerId + ":" + idempotencyKey), result -> {});
            Event existing = repository.findByOwnerIdAndIdempotencyKey(ownerId, idempotencyKey).orElse(null);
            if (existing != null) {
                if (!existing.getEventType().equals(request.type().trim())
                        || !existing.getPayload().equals(request.payload())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency key already used for a different event");
                }
                return new Creation(EventResponse.from(existing), true);
            }
        }

        Event event = Event.create(
                request.type(),
                request.payload(),
                ownerId,
                idempotencyKey
        );

        Event saved = repository.save(event);
        deliveryService.createPendingFor(saved);

        return new Creation(EventResponse.from(saved), false);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAll(UUID ownerId) {
        return repository.findAllByOwnerId(ownerId)
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse findById(UUID ownerId, UUID id) {

        Event event = repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));

        return EventResponse.from(event);
    }
}
