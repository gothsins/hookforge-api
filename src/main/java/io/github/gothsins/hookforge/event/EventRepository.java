package io.github.gothsins.hookforge.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository
        extends JpaRepository<Event, UUID> {
}