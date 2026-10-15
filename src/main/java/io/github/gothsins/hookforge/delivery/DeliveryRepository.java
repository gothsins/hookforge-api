package io.github.gothsins.hookforge.delivery;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    List<Delivery> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<Delivery> findAllByOwnerIdAndEventIdOrderByCreatedAtDesc(UUID ownerId, UUID eventId);
    Optional<Delivery> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Delivery> findAllByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            List<DeliveryStatus> statuses, Instant dueAt, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.id = :id and d.ownerId = :ownerId")
    Optional<Delivery> findLocked(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
