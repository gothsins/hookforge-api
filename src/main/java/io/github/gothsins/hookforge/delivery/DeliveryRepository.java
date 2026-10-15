package io.github.gothsins.hookforge.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    List<Delivery> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<Delivery> findAllByOwnerIdAndEventIdOrderByCreatedAtDesc(UUID ownerId, UUID eventId);
    Optional<Delivery> findByIdAndOwnerId(UUID id, UUID ownerId);
}
