package io.github.gothsins.hookforge.event;

import io.github.gothsins.hookforge.event.dto.CreateEventRequest;
import io.github.gothsins.hookforge.event.dto.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repository;

    @Transactional
    public EventResponse create(UUID ownerId, CreateEventRequest request) {

        Event event = Event.create(
                request.type(),
                request.payload(),
                ownerId
        );

        Event saved = repository.save(event);

        return EventResponse.from(saved);
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
