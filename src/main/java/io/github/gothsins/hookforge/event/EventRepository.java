package io.github.gothsins.hookforge.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository
        extends JpaRepository<Event, UUID> {
    List<Event> findAllByOwnerId(UUID ownerId);
    Optional<Event> findByIdAndOwnerId(UUID id, UUID ownerId);
}
