package io.github.gothsins.hookforge.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    private Event(String eventType, Map<String, Object> payload) {
        this.eventType = eventType.trim();
        this.payload = new LinkedHashMap<>(payload);
        this.receivedAt = Instant.now();
    }

    public static Event create(
            String eventType,
            Map<String, Object> payload
    ) {
        return new Event(eventType, payload);
    }
}