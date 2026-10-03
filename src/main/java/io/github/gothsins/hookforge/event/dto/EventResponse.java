package io.github.gothsins.hookforge.event.dto;

import io.github.gothsins.hookforge.event.Event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String type,
        Map<String, Object> payload,
        Instant receivedAt
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getEventType(),
                event.getPayload(),
                event.getReceivedAt()
        );
    }
}